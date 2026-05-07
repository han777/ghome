package com.apartment.security;

import com.apartment.entity.SysUser;
import com.apartment.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    private SysUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .or(() -> userRepository.findByPhone(login))
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with login: " + login));
        return new User(user.getUsername(), user.getPassword(),
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getRoleCode())).collect(Collectors.toList()));
    }
}
