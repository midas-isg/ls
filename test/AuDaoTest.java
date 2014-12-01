import org.junit.Test;

import dao.AuDao;
import dao.entities.AdministrativeUnit;


public class AuDaoTest {
	@Test
	public void testFindAll() throws Exception {
		AuDao dao = new AuDao();
		AdministrativeUnit au = dao.read(1);
		System.out.println(au);
	}
}
