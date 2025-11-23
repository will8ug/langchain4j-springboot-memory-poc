package io.will.langchain4jpoc.memory.mem0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to manage query context across reactive and non-reactive execution paths.
 * 
 * This class provides a mechanism to store and retrieve queries that need to be passed
 * to memory search operations. It supports:
 * 1. ThreadLocal storage (primary mechanism for cross-thread access)
 * 2. Reactor Context integration for reactive chains
 * 
 * The query is typically set at the controller level and retrieved when LangChain4j
 * calls the memory's messages() method, which may execute on a different thread.
 * 
 * Usage:
 * - In reactive chains: Use propagateContext() operator to copy Reactor Context to ThreadLocal
 * - In non-reactive code: Use setQuery() and getQuery() directly
 */
public class QueryContext {
    private static final Logger logger = LoggerFactory.getLogger(QueryContext.class);
    
    public static final String QUERY_CONTEXT_KEY = "query";
    
    private static final ThreadLocal<Map<Object, String>> threadLocalQueries = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    /**
     * Stores a query for a given memory ID in ThreadLocal storage.
     */
    public static void setQuery(Object memoryId, String query) {
        threadLocalQueries.get().put(memoryId, query);
        logger.debug("Set query for memory ID {} in ThreadLocal: {}", memoryId, query);
    }
    
    /**
     * Retrieves the query for a given memory ID from ThreadLocal storage.
     */
    public static String getQuery(Object memoryId) {
        Map<Object, String> threadQueries = threadLocalQueries.get();
        String query = threadQueries.get(memoryId);
        if (query != null) {
            logger.debug("Retrieved query from ThreadLocal for memory ID {}: {}", memoryId, query);
        }
        return query;
    }
    
    /**
     * Clears the query for a given memory ID from ThreadLocal storage.
     * 
     * @param memoryId The memory ID
     */
    public static void clearQuery(Object memoryId) {
        threadLocalQueries.get().remove(memoryId);
        logger.debug("Cleared query for memory ID: {}", memoryId);
    }
    
    /**
     * Clears all queries from ThreadLocal storage.
     * This should be called at the end of a request to prevent memory leaks.
     */
    public static void clearThreadLocal() {
        threadLocalQueries.get().clear();
        logger.debug("Cleared ThreadLocal queries");
    }
    
    /**
     * Creates a Reactor Context with the query stored for the given memory ID.
     * This should be used with contextWrite() in reactive chains.
     * 
     * @param memoryId The memory ID
     * @param query The query string
     * @return A Context object that can be used with contextWrite()
     */
    public static Context createContext(Object memoryId, String query) {
        Map<Object, String> queries = new ConcurrentHashMap<>();
        queries.put(memoryId, query);
        return Context.of(QUERY_CONTEXT_KEY, queries);
    }
    
    /**
     * Retrieves the query from a Reactor ContextView.
     * 
     * @param contextView The Reactor ContextView
     * @param memoryId The memory ID
     * @return The query string, or null if not found
     */
    public static String getQueryFromContext(ContextView contextView, Object memoryId) {
        if (contextView.hasKey(QUERY_CONTEXT_KEY)) {
            @SuppressWarnings("unchecked")
            Map<Object, String> queries = contextView.get(QUERY_CONTEXT_KEY);
            return queries.get(memoryId);
        }
        return null;
    }
    
    /**
     * Propagates queries from a Reactor ContextView to ThreadLocal.
     * This is useful when you need to propagate context before executing code
     * that might run on a different thread.
     * 
     * @param contextView The Reactor ContextView
     */
    public static void propagateFromContext(ContextView contextView) {
        if (contextView != null && contextView.hasKey(QUERY_CONTEXT_KEY)) {
            @SuppressWarnings("unchecked")
            Map<Object, String> queries = contextView.get(QUERY_CONTEXT_KEY);
            Map<Object, String> threadQueries = threadLocalQueries.get();
            threadQueries.putAll(queries);
            logger.debug("Propagated {} queries from Reactor Context to ThreadLocal", queries.size());
        }
    }
    
    /**
     * Propagates queries from Reactor Context to ThreadLocal.
     * This should be used as an operator in reactive chains to ensure queries
     * are available when LangChain4j memory methods are called on different threads.
     * 
     * Usage: .transform(QueryContext::propagateContext)
     * 
     * This operator propagates context on each signal, ensuring queries are available
     * when LangChain4j memory methods are called, even when execution happens on
     * different threads.
     * 
     * @param <T> The type of the reactive stream
     * @return A transformer function that propagates context to ThreadLocal
     */
    public static <T> reactor.core.publisher.Flux<T> propagateContext(reactor.core.publisher.Flux<T> flux) {
        return flux.doOnEach(signal -> {
            try {
                ContextView ctx = signal.getContextView();
                if (ctx != null && ctx.hasKey(QUERY_CONTEXT_KEY)) {
                    @SuppressWarnings("unchecked")
                    Map<Object, String> queries = ctx.get(QUERY_CONTEXT_KEY);
                    // Copy all queries from context to ThreadLocal
                    Map<Object, String> threadQueries = threadLocalQueries.get();
                    threadQueries.putAll(queries);
                    logger.debug("Propagated {} queries from Reactor Context to ThreadLocal on signal", queries.size());
                }
            } catch (Exception e) {
                // Context not available on this signal, ignore
                logger.trace("Could not get context from signal: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Propagates queries from Reactor Context to ThreadLocal for Mono.
     * 
     * This operator propagates context on each signal, ensuring queries are available
     * when LangChain4j memory methods are called, even when execution happens on
     * different threads.
     * 
     * @param <T> The type of the reactive stream
     * @return A transformer function that propagates context to ThreadLocal
     */
    public static <T> reactor.core.publisher.Mono<T> propagateContext(reactor.core.publisher.Mono<T> mono) {
        return mono.doOnEach(signal -> {
            try {
                ContextView ctx = signal.getContextView();
                if (ctx != null && ctx.hasKey(QUERY_CONTEXT_KEY)) {
                    @SuppressWarnings("unchecked")
                    Map<Object, String> queries = ctx.get(QUERY_CONTEXT_KEY);
                    // Copy all queries from context to ThreadLocal
                    Map<Object, String> threadQueries = threadLocalQueries.get();
                    threadQueries.putAll(queries);
                    logger.debug("Propagated {} queries from Reactor Context to ThreadLocal on signal", queries.size());
                }
            } catch (Exception e) {
                // Context not available on this signal, ignore
                logger.trace("Could not get context from signal: {}", e.getMessage());
            }
        });
    }
}

