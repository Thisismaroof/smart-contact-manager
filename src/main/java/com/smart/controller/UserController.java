package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	private final DaoAuthenticationProvider authenticationProvider;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	UserController(DaoAuthenticationProvider authenticationProvider) {
		this.authenticationProvider = authenticationProvider;
	}

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("Username :" + userName);
		User user = userRepository.getUserByUserName(userName);
		System.out.println("User :" + user);
		model.addAttribute("user", user);
	}

//dashboard_home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User_DashBoard");
		return "normal/user_dashboard";
	}

//Open Form Handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	// Processing Add Contact Form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, RedirectAttributes redirectAttributes) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// File upload
			if (file.isEmpty()) {
				System.out.println("File Is Empty");
				contact.setImage("contact.png");

			} else {
				contact.setImage(file.getOriginalFilename());
				File savefile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			contact.setUser(user);
			user.getContacts().add(contact);

			this.userRepository.save(user);

			redirectAttributes.addFlashAttribute("message",
					new Message("Your Contact is Added Successfully!!!", "success"));

		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("message", new Message("Something went wrong! Try again.", "danger"));
		}

		return "redirect:/user/add-contact";
	}

	// show contact handler
	@GetMapping("/show_contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		// current page - page
		// Contact Per page -5
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

//Showing Specific contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID" + cId);
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("title", contact.getName());
			model.addAttribute("contact", contact);
		}
		return "normal/contact_detail";
	}

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cid, Principal principal,
			RedirectAttributes redirectAttributes) {

		Contact contact = this.contactRepository.findById(cid).get();

		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);
		this.userRepository.save(user);

		redirectAttributes.addFlashAttribute("message", new Message("Contact Deleted Successfully....", "success"));

		return "redirect:/user/show_contacts/0";

	}

	// open update form handler
	@PostMapping("/update-contact/{cId}")
	public String updateContact(@PathVariable("cId") Integer cId, Model m) {
		m.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cId).get();
		m.addAttribute("contact", contact);
		return "normal/update_contact_form";
	}

	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHander(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession httpSession, Principal principal) {

		try {
			Contact oldcontactDetail = this.contactRepository.findById(contact.getCId()).get();
			if (!file.isEmpty()) {
				// delete old photo
				// update new photo

				// Delete Photo
				File deletefile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deletefile, oldcontactDetail.getImage());
				file1.delete();

				// Update Photo
				File savefile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(savefile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			} else {
				contact.setImage(oldcontactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			httpSession.setAttribute("message", new Message("Your Contact is Updated....", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/" + contact.getCId() + "/contact";
	}

	// Your Profile Setting

	@GetMapping("/profile")
	public String yourProfile(Model m) {
		m.addAttribute("title", "Profile Page");
		return "normal/profile";
	}

	// open settings handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldpassword,
			@RequestParam("newPassword") String newpassword, Principal principal,
			RedirectAttributes redirectAttributes) {
		System.out.println("Old Password :" + oldpassword);
		System.out.println("New Password :" + newpassword);

		String Name = principal.getName();
		User current_user = this.userRepository.getUserByUserName(Name);
		System.out.println(current_user.getPassword());

		if (this.bCryptPasswordEncoder.matches(oldpassword, current_user.getPassword())) {
			current_user.setPassword(this.bCryptPasswordEncoder.encode(newpassword));
			this.userRepository.save(current_user);
			redirectAttributes.addFlashAttribute("message",
					new Message("Your Password has been Changed Successfully....", "success"));
		} else {
			redirectAttributes.addFlashAttribute("message",
					new Message("Your Old Password is Incorrect....", "warning"));
			return "redirect:/user/settings";
		}

		return "redirect:/user/index";
	}

}
