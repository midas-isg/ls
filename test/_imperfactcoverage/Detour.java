package _imperfactcoverage;

import play.libs.F.Function0;

public class Detour {
	private Detour() {
	}

	public static <T> T wrapNoThrowingCheckedExecption(Function0<T> block) {
		try {
			return block.apply();
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
