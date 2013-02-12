/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.guacamole.net.auth.mysql;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.mysql.dao.guacamole.UserMapper;

/**
 * A MySQL based implementation of the User Directory.
 * @author James Muehlner
 */
public class UserDirectory implements Directory<String, User> {

    private Map<String, User> userMap = new HashMap<String, User>();
    
    @Inject
    UserMapper userDAO;
    
    @Inject
    Provider<MySQLUser> mySQLUserProvider;
    
    private MySQLUser getMySQLUser(User user) {
        MySQLUser mySQLUser = mySQLUserProvider.get();
        mySQLUser.init(user);
        return mySQLUser;
    }
    
    @Override
    public User get(String identifier) throws GuacamoleException {
        return userMap.get(identifier);
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return userMap.keySet();
    }

    @Override
    public void add(User object) throws GuacamoleException {
        MySQLUser mySQLUser = getMySQLUser(object);
        userDAO.insert(mySQLUser.getUser());
        userMap.put(mySQLUser.getUsername(), mySQLUser);
    }

    @Override
    public void update(User object) throws GuacamoleException {
        if(!userMap.containsKey(object.getUsername()))
            throw new GuacamoleException("User not found in Directory.");
        MySQLUser mySQLUser = getMySQLUser(object);
        userDAO.updateByPrimaryKey(mySQLUser.getUser());
        userMap.put(object.getUsername(), mySQLUser);
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        User user = userMap.get(identifier);
        if(user == null)
            throw new GuacamoleException("User not found in Directory.");
        MySQLUser mySQLUser = getMySQLUser(user);
        userDAO.deleteByPrimaryKey(mySQLUser.getUser().getUser_id());
        userMap.remove(user.getUsername());
    }
}
