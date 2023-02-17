package com.start.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.start.dao.UserRepository;
import com.start.entities.User;

public class UserDetailsServiceImpl implements UserDetailsService{

	@Autowired
	private UserRepository userRepository;
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		//Fetching user from database
		User user =userRepository.getUserByUserName(username);
		if(user==null)
		{
			throw new UsernameNotFoundException("Could not found user!!");
		}
		
		CustomUserDetail customUserDetails = new CustomUserDetail(user);
		
		return customUserDetails;
	}

}
