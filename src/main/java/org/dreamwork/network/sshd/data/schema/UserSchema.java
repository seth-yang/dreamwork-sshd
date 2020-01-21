package org.dreamwork.network.sshd.data.schema;

import org.dreamwork.persistence.*;

public class UserSchema extends DatabaseSchema {
	public UserSchema () {
		tableName = "t_user";
		fields = new String[] {"user_name", "password"};
	}

	@Override
	public String getPrimaryKeyName () {
		return "user_name";
	}

	@Override
	public String getCreateDDL () {
		return "CREATE TABLE t_user (" +
"    user_name       TEXT        PRIMARY KEY," +
"    password        TEXT" +
")";
	}
}