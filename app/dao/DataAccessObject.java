package dao;

import java.util.ArrayList;
import java.util.List;

import dao.entities.Entity;
import gateways.database.jpa.JpaAdaptor;

public class DataAccessObject<T extends Entity> {
	private Class<T> clazz;
	private JpaAdaptor adaptor;

	public DataAccessObject(Class<T> clazz, JpaAdaptor adaptor) {
		this.clazz = clazz;
		this.adaptor = adaptor;
	}

	public List<T> findAll() {
		return adaptor.query(clazz);
	}

	public Long create(T data) {
		return adaptor.create(data);
	}

	public T read(long id) {
		return adaptor.read(clazz, id);
	}

	public T update(long id, T data) {
		return adaptor.update(clazz, id, data);
	}

	public void delete(long id) {
		adaptor.delete(clazz, id);
	}

	public List<Long> createAll(List<T> data) {
		List<Long> result = new ArrayList<>();
		if (data == null)
			return null;
		for (T item : data)
			result.add(create(item));
		return result;
	}

	public void deleteAll(List<T> data) {
		if (data == null)
			return;
		for (T item : data)
			delete(item.getId());
	}

}
