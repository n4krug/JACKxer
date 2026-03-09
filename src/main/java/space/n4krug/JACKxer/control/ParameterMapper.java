package space.n4krug.JACKxer.control;

@FunctionalInterface
public interface ParameterMapper<T> {
    T map(float normalized);
}