package common;

public class RuntimeExceptionReThrower {
    private RuntimeExceptionReThrower(){
    }

    public static <R> R tryTo(Function<R> function) {
        return tryTo(null, function);
    }

    public static <R> R tryTo(String message, Function<R> function) {
        try {
            return function.call();
        } catch (RuntimeException|Error e) {
            throw e;
        } catch (Throwable th) {
            throw new RuntimeException(message, th);
        }
    }

    public static Function<Void> returnVoid(Procedure p) {
        return () -> {
            p.call();
            return null;
        };
    }
}