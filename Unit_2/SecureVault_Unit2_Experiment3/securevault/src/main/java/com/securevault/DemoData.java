package com.securevault;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@DependsOnDatabaseInitialization
@ConditionalOnProperty(name="app.demo",havingValue="true")
public class DemoData implements InitializingBean {
    private final Store store; private final PasswordEncoder passwords;
    public DemoData(Store store,PasswordEncoder passwords) { this.store=store; this.passwords=passwords; }
    @Override public void afterPropertiesSet() {
        seed("admin@securevault.local","Tanish · Admin","Admin@12345","ADMIN");
        seed("user@securevault.local","Demo User","User@12345","USER");
    }
    private void seed(String email,String name,String password,String role) {
        if(store.byEmail(email).isEmpty()) store.createUser(email,name,passwords.encode(password),role);
    }
}
