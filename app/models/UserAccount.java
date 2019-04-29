package models;

import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;

public class UserAccount {
	private String email;
	private String hashedPassword;
	
	public UserAccount(String email, String password) {
		this.email = email;
		hashedPassword = toMD5(password);
		
		return;
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
	
	public void setPassword(String password) {
		hashedPassword = toMD5(password);
		
		return;
	}
	
	public boolean passwordMatch(String password) {
		return (toMD5(password) == hashedPassword);
	}
	
	/*
	public double getLongitude() {
		return coordinates[1];
	}

	public void setLongitude(double ordinate) {
		this.coordinates[1] = ordinate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(coordinates);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (!Arrays.equals(coordinates, other.coordinates))
			return false;
		return true;
	}
	*/
}
