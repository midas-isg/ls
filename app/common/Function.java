package common;

public interface Function<R>{
    R call() throws Throwable;
}