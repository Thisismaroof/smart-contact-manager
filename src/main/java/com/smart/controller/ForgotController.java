package com.smart.controller;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.service.EmailService;


@Controller
public class ForgotController {
	Random random = new Random();

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@RequestMapping("/forgot")
	public String opemEmailForm() {
		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, Model model) {

		User user = userRepository.getUserByUserName(email);

		if (user == null) {
			model.addAttribute("message", new Message("User not found with this email", "danger"));
			return "forgot_email_form";
		}

		int otp = 100000 + random.nextInt(900000);

		user.setOtp(otp);
		user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
		userRepository.save(user);

		emailService.sendEmail("OTP from Smart Contact Manager", "<h1>Your OTP is " + otp + "</h1>", email);

		model.addAttribute("email", email);
		return "verify_otp";
	}

	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("email") String email, @RequestParam("otp") Integer otp, Model model) {

		User user = userRepository.getUserByUserName(email);

		if (user == null) {
			model.addAttribute("message", new Message("Invalid request", "danger"));
			return "forgot_email_form";
		}

		if (user.getOtp() == null || !user.getOtp().equals(otp) || user.getOtpExpiry().isBefore(LocalDateTime.now())) {

			model.addAttribute("message", new Message("Invalid or expired OTP", "danger"));
			model.addAttribute("email", email);
			return "verify_otp";
		}

		model.addAttribute("email", email);
		return "password_change_form";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("email") String email,
	                             @RequestParam("newPassword") String newPassword,
	                             Model model) {

	    User user = userRepository.getUserByUserName(email);

	    user.setPassword(passwordEncoder.encode(newPassword));

	    // clear OTP
	    user.setOtp(null);
	    user.setOtpExpiry(null);

	    userRepository.save(user);

	    model.addAttribute("message",
	        new Message("Password changed successfully!", "success"));

	    return "login";
	}


}
