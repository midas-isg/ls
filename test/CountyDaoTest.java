import java.util.List;

import org.junit.Test;

import dao.CountyDAO;
import dao.entities.County;


public class CountyDaoTest {
	@Test
	public void testFindAll() throws Exception {
		CountyDAO dao = new CountyDAO();
		List<County> list = dao.findAllCounties();
		System.out.println(list);
	}
}
