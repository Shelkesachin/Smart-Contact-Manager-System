package com.start.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.start.dao.ContactRepository;
import com.start.dao.UserRepository;
import com.start.entities.Contact;
import com.start.entities.User;
import com.start.helper.Message;

@Controller
@RequestMapping("/user")

public class UserController 
{
	@Autowired
	private UserRepository userRepsository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//method for adding common
	@ModelAttribute
	public void addCommonData(Model model,Principal principal)
	{

		String userName = principal.getName();
		System.out.println("USERNAME "+userName);
		
		
		//get the user using username(Email)
		User user = userRepsository.getUserByUserName(userName);
		
		System.out.println("USER "+user);
		
		model.addAttribute("user",user);
		
		
	}
	
	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal)
	{
		
		return "normal/user_dashboard";
	}
	
	//Open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model)
	{
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		
		
		return "normal/add_contact_form";
	}
	
	
	@PostMapping("/process-contact")
	//Processing add contact form
	public String processContact(@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal,HttpSession session)
	{
		
		try
		{
			String name=principal.getName();
			User user= this.userRepsository.getUserByUserName(name);

			
			
			
			
			//Processing and uploading file
			
			if(file.isEmpty())
			{
				//if file is empty then try our msg
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}
			else
			{
				//file the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				
				File saveFile= new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(),path ,StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is uploaded");
			}
			
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepsository.save(user);
			
			System.out.println("Added to data base");
			
			System.out.println("DATA "+contact);
			
			//Message success.....
			session.setAttribute("message", new Message("Your contact is added !! Add more..", "success"));
			
		}
		catch(Exception e)
		{
			System.out.println("Error "+e.getMessage());
			e.printStackTrace();
			
			//Message error
			session.setAttribute("message", new Message("Something Went Wrong! Try Again.", "danger"));
			
			
		}
		
		
		return "normal/add_contact_form";
	}
	
	//Show contacts handler
	//Per page = 5[n]
	//current page = 0 [page]
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page")Integer page,Model m,Principal principal)
	{
		m.addAttribute("title","Show user concats");
		
		//Contact ki list ko bhejni hai
		String userName = principal.getName();
		
		User user= this.userRepsository.getUserByUserName(userName);
		
		
		PageRequest pageable = PageRequest.of(page, 5);
		Page<Contact> contacts=	this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		
		m.addAttribute("contacts",contacts);
		
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		
		return "normal/show_contacts";
	}
	
	//Showing particular contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId")Integer cId,Model model,Principal principal)
	{
	
		Optional<Contact> contactOptional =this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		//
		String userName = principal.getName();
		User user = this.userRepsository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	// delete contact handler

		@GetMapping("/delete/{cid}")
		@Transactional
		public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,
				Principal principal) {
			
			Contact contact = this.contactRepository.findById(cId).get();
			
			//check
			//System.out.println("Contact "+contact.getCid());
			
			//contact.setUser(null);
			
			User user = this.userRepsository.getUserByUserName(principal.getName());

			user.getContacts().remove(contact);

			this.userRepsository.save(user);

			System.out.println("DELETED");
			session.setAttribute("message", new Message("Contact deleted succesfully...", "success"));

			return "redirect:/user/show-contacts/0";
			}
	
		
		//open update form handler
		@PostMapping("/update-contact/{cid}")
		public String updateForm(@PathVariable("cid") Integer cid,Model m)
		{
			
			m.addAttribute("title","Update Contact");
			
			Contact contact =this.contactRepository.findById(cid).get();
			
			m.addAttribute("contact",contact);
			
			return "normal/update_form";
		}
		
		
		// update contact handler
		@RequestMapping(value = "/process-update", method = RequestMethod.POST)
		public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
				Model m, HttpSession session, Principal principal) {

			try {

				// old contact details
				Contact oldcontactDetail = this.contactRepository.findById(contact.getCid()).get();

				// image..
				if (!file.isEmpty()) {
					// file work..
					// rewrite

//					delete old photo

					File deleteFile = new ClassPathResource("static/img").getFile();
					File file1 = new File(deleteFile, oldcontactDetail.getImage());
					file1.delete();

//					update new photo

					File saveFile = new ClassPathResource("static/img").getFile();

					Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

					Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

					contact.setImage(file.getOriginalFilename());

				} else {
					contact.setImage(oldcontactDetail.getImage());
				}

				User user = this.userRepsository.getUserByUserName(principal.getName());

				contact.setUser(user);

				this.contactRepository.save(contact);

				session.setAttribute("message", new Message("Your contact is updated...", "success"));

			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println("CONTACT NAME " + contact.getName());
			System.out.println("CONTACT ID " + contact.getCid());
			return "redirect:/user/" + contact.getCid() + "/contact";
		}
		
		// your profile handler
		@GetMapping("/profile")
		public String yourProfile(Model model) {
			model.addAttribute("title", "Profile Page");
			return "normal/profile";
		}

}
