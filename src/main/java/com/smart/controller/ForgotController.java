package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
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

import jakarta.servlet.http.HttpSession;


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
	public String sendOtp(@RequestParam("email") String email, HttpSession session) {
		System.out.println("Email :" + email);

		// generating a 4 digit OTP

		int otp = 100000 + random.nextInt(900000);
		System.out.println(otp);

		String subject = "OTP from Smart Contact Manager";
		String message = "<h1> OTP = " + otp + "</h1>";
		String to = email;

		boolean flag = this.emailService.sendEmail(subject, message, to);

		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			session.setAttribute("otpSent", true);
			return "verify_otp";

		} else {
			session.setAttribute("message", new Message("Check You Email ID", "danger"));
			return "forgot_email_form";
		}

	}

	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") Integer otp, HttpSession session, Model model) {

		Integer myotp = (Integer) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (myotp != null && myotp.equals(otp)) {

			User user = this.userRepository.getUserByUserName(email);
			if (user == null) {
				// send error message
				model.addAttribute("message", new Message("User does not Exist with this Email...!", "danger"));
				return "forgot_email_form";
			} else {
				// send change Password form
			}

			return "password_change_form";
		} else {
			model.addAttribute("message", new Message("You have entered a wrong OTP!", "danger"));
			return "verify_otp"; // same page
		}
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session) {
		String email = (String) session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.passwordEncoder.encode(newpassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=Password Changed Successfully....!";
	}

}
