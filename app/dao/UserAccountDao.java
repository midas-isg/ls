package dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import dao.entities.UserAccount;
import gateways.database.jpa.JpaAdaptor;
import play.db.jpa.JPA;

public class UserAccountDao extends DataAccessObject<UserAccount> {
	public UserAccountDao(EntityManager em) {
		this(new JpaAdaptor(em));
	}

	private UserAccountDao(JpaAdaptor jpaAdaptor) {
		super(UserAccount.class, jpaAdaptor);
	}
	
	@Override
	public List<UserAccount> findAll() {
		EntityManager em = JPA.em();
		Query query = em.createQuery("from " + UserAccount.class.getSimpleName());
		@SuppressWarnings("unchecked")
		List<UserAccount> result = (List<UserAccount>)query.getResultList();
		return result;
	}
}
