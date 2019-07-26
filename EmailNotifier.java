package controllers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pojos.NotifierResponse;

@Controller
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EmailNotifier {
	private static final Logger LOG = LogManager.getLogger(EmailNotifier.class);

	@Value("${spring.mail.host}")
	private String server;

	@Value("${spring.mail.port}")
	private String port;

	@Value("${spring.mail.username}")
	private String username;

	@Value("${spring.mail.password}")
	private String password;
	
	@Value("${emailTo}")
	private String emailTo;
	
	@Value("${emailFrom}")
	private String emailFrom;
	
	@Value("${emailFromName}")
	private String emailFromName;
	
	@Value("${emailToName}")
	private String emailToName;
	
	@Value("${subject}")
	private String subject;

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping("email")
	public String mailPage() {
		return "email";
	}

	@PostMapping(value = "notify")
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	public ResponseEntity<NotifierResponse> sendMail(@RequestBody String body) {
		final Map<String, String> formMap = new HashMap<String, String>();
		final NotifierResponse notifierResponse = new NotifierResponse("Accepted", HttpStatus.ACCEPTED, null);
		JSONObject jsonBody = null;
		try {
			jsonBody = new JSONObject(body);
		} catch (JSONException e) {
			notifierResponse.setException(e);
			notifierResponse.setMessage("Error paring json");
			notifierResponse.setStatusCode(HttpStatus.BAD_REQUEST);
			LOG.error("Error parsing json in EmailNotifier method sendMail", e);
			return new ResponseEntity<NotifierResponse>(notifierResponse, notifierResponse.getStatusCode());
		}

		Iterator<String> iterator = jsonBody.keys();
		while (iterator.hasNext()) {
			String k = iterator.next();
			String v = String.valueOf(jsonBody.get(k));
			formMap.put(k, v);
		}
		buildEmail(subject, formMap);
		return new ResponseEntity<NotifierResponse>(notifierResponse, notifierResponse.getStatusCode());
	}

	private void buildEmail(String subject, final Map<String, String> map) {
		final StringBuilder builder = new StringBuilder();
		map.forEach((k, v) -> {
			String column1 = wrapTag("td", k);
			String column2 = wrapTag("td", v);
			String row = wrapTag("tr", column1 + column2);
			builder.append(row);
		});
		
		final String body = wrapTag("tbody", builder.toString());
		final String table = wrapTag("table", body);
		final String htmlBody = wrapTag("body", table);
		final String head = wrapTag("head", "<title></title>");
		final String html = wrapTag("html", head + htmlBody);
		
		Email email = EmailBuilder.startingBlank()
								  .from(emailFromName, emailFrom)
								  .to(emailToName, emailTo)
								  .withSubject(subject)
								  .withHTMLText(html)
								  .buildEmail();

		MailerBuilder.withSMTPServer(server, Integer.parseInt(port), username, password)
				.withTransportStrategy(TransportStrategy.SMTPS)
				.buildMailer()
				.sendMail(email);
	}
	
	private String wrapTag(String tag, String content) {
		String element = "";
		element += "<" + tag + ">";
		element += content;
		element += "</" + tag + ">";
		return element;
	}

}
