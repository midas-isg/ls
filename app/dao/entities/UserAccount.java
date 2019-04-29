package dao.entities;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_account")
public class UserAccount implements dao.entities.Entity {
	private Long id;
	private String email;
	private String hashedPassword;
	
	@Id
	@Column(columnDefinition = "serial")
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	private byte[] getSalt(String input) {
		char [] char_string = input.toCharArray();
		int half_length = input.length() >> 1;
		int last_index = input.length() - 1;
		char temp;
		
		for(int i = 0; i < half_length; i++) {
			temp = char_string[i];
			char_string[i] = char_string[last_index - i];
			char_string[last_index - i] = temp;
		}
		
		return new String(char_string).getBytes();
	}
	
	private String toMD5(String inputString) {
		String md5String = null;
		
		try {
			// Static getInstance method is called with hashing MD5
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			md.update(getSalt(inputString));
			
			// digest() method is called to calculate message digest
			//  of an input digest() return array of byte 
			byte[] messageDigest = md.digest(inputString.getBytes());
			
			// Convert byte array into signum representation
			BigInteger no = new BigInteger(1, messageDigest);
			
			// Convert message digest into hex value
			md5String = no.toString(16);
			
			while (md5String.length() < 32) {
				md5String = "0" + md5String;
			}
		}
		catch(NoSuchAlgorithmException md5Exception) {
			throw new RuntimeException(md5Exception);
		}
		
		return md5String;
	}
	
	@Id
	@Column(name = "email")
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setPassword(String password) {
		hashedPassword = toMD5(password);
	}
	
	public boolean passwordMatches(String password) {
		return (toMD5(password) == hashedPassword);
	}
	
	@Override
	public String toString() {
		//return "SuperType [id=" + id + ", name=" + name + ", userDefinable=" + userDefinable + "]";
		return "UserAccount [email=" + email + "]";
	}
}
