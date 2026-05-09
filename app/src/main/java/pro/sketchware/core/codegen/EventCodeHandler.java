package pro.sketchware.core.codegen;

/**
 * Functional interface for generating event handler code templates.
 * Implementations are registered in {@link EventCodeRegistry}.
 */
@FunctionalInterface
public interface EventCodeHandler {
    /**
     * @param targetId   the target component/view ID
     * @param eventLogic the generated Java code for the event body
     * @return complete event handler method code
     */
    String generate(String targetId, String eventLogic);
}
