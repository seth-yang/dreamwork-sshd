package org.dreamwork.network.sshd.data;

import org.dreamwork.network.sshd.data.schema.UserSchema;
import org.dreamwork.persistence.*;

@ISchema (UserSchema.class)
public class User {
	@ISchemaField (id = true, name = "user_name")
	private String userName;

	@ISchemaField (name = "password")
	private String password;

	public String getUserName () {
		return userName;
	}

	public void setUserName (String userName) {
		this.userName = userName;
	}

	public String getPassword () {
		return password;
	}

	public void setPassword (String password) {
		this.password = password;
	}

	@Override
	public boolean equals (java.lang.Object o) {
		if (this == o) return true;
		if (o == null || getClass () != o.getClass ()) return false;
		User that = (User) o;
		return userName != null && userName.equals (that.getUserName ());
	}

	@Override
	public int hashCode () {
		return String.valueOf (userName).hashCode ();
	}
}