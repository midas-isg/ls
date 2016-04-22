package _imperfactcoverage;

import play.libs.F.Function0;
//import integrations.app.controllers.TestCoordinate;

public class Detour {
	private Detour(){
	}

	/*public static Callback0 testLimitWithNegative(TestCoordinate that) {
		return () -> {
			try {
				that.testLimit(-1, 0);
				Assert.fail();
			} catch (Exception e){
				assertThat(e).isInstanceOf(Exception.class);
			}
		};
	}*/
	
	public static <T> T wrapNoThrowingCheckedExecption(Function0<T> block) {
		try {
			return block.apply();
		} catch (RuntimeException|Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
