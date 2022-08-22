package greeting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import greeting.objects.Quote;
import io.opentracing.util.GlobalTracer;

@SpringBootApplication
@RestController
@ComponentScan( basePackages = {"greeting"} )
public class Application {

    private static Logger logger = LoggerFactory.getLogger(Application.class);
    private static enum DEVICE_TYPE {
	MOBILE, WEB;
    }
    
    @Value("${profile.service.base-url:http://localhost:8081}")
    private String profileServiceBaseUrl;
    
    
    @RequestMapping("/greeting/{userid}")
    public String greeting(@PathVariable("userid") int userid, @RequestParam(required = false) String deviceType) throws Exception {
	
	Account account = getAccountByUserId(userid);
	String accountGreeting = getAccountGreeting(account);
	DEVICE_TYPE sourceDeviceType = getSourceDeviceForRequest(deviceType);
	
	
	if(account.isPlatinum()) {
	    logger.info(account.getAccountName() + " is platinum, loading custom greeting");
	    
	    int creditsRemaining = getCreditsRemaining(account);
	    return String.format("Welcome, %s! And thank you for being a platinum customer. You have %s credits remaining. %s", 
		    account.getAccountName(), 
		    creditsRemaining, 
		    accountGreeting);    
	} else { 
	    //Non-loyalty member
	    if(sourceDeviceType.equals(DEVICE_TYPE.MOBILE)) {
		logger.info("Loading mobile greetings for account " + account.getAccountName());
		return String.format("Hi %s!", account.getAccountName());
	    }
    	    logger.info("Loading standard greeting for " + account.getAccountName());
    	    return String.format("Welcome, %s! %s", account.getAccountName(), accountGreeting);
	}
    }
 
    
    @RequestMapping("/kaboom")
    public String explode() throws Exception {
	throw new Exception("Kaboom");
    }
  
    @RequestMapping("/")
    public String home() throws Exception {
	return "Please use /greeting/{userid}";
    }
    
    private String getUsernameFromCustomerProfileService(int userid) throws UserNotFoundException {
	String usernameEndpoint = String.format("%s/username/%s", profileServiceBaseUrl, userid);
	String username = createTracedRestTemplate().getForObject(usernameEndpoint, String.class);
	if (null == username) {
	    throw new UserNotFoundException(String.format("Unable to find user by id: %s", userid));
	}
	return username;
    }

    private String getAccountGreeting(Account account) {
//	RestTemplate restTemplate = createTracedRestTemplate();
//	Quote quote = restTemplate.getForObject("http://gturnquist-quoters.cfapps.io/api/random", Quote.class);
//	GlobalTracer.get().activeSpan().setTag("QuoteType", quote.getType());
//	return quote.toString();
	return "Static quote.";
    }
    
    private DEVICE_TYPE getSourceDeviceForRequest(String deviceType) {
	if (deviceType != null) {
	    return DEVICE_TYPE.MOBILE;
	}
	if(returnFalseWithProbability(70.0)) {
	    return DEVICE_TYPE.MOBILE;
	}
	return DEVICE_TYPE.WEB;
    }

    
    private RestTemplate createTracedRestTemplate() {
	return new RestTemplate();
    }
    
    private String getAnonymousUsername() {
	try {
	    return getUsernameFromCustomerProfileService(-1);
	} catch(UserNotFoundException unfe) {
	    logger.error("Unable to find the anonymous user: -1");
	}
	return null;
    }

    private void waitForRandomDuration() throws Exception {
	    long duration = getRandomDuration();
	    sleepForMillis(duration);    
    }

    private void sleepForMillis(long durationInMillis) throws Exception {
	    try {
		Thread.sleep(durationInMillis);
	    } catch(InterruptedException ie) {
		// do nothing
	    }	
    }

    private boolean returnFalseWithProbability(Double probability) {
	Double greaterThanValue = (100-probability)/100;
	return Math.random() > greaterThanValue;
    }

    private static long MAX_DURATION = 1*200L;    //200ms
    private static long getRandomDuration() {
	return (long) (Math.random() * MAX_DURATION);
    }

    public Application() {
	super();
    }
    
    public static void main(String[] args) {

	SpringApplication.run(Application.class, args);
    }

    private class UserNotFoundException extends Exception {
	private static final long serialVersionUID = 3067280656945130852L;
	
	public UserNotFoundException(String name) {
	    super(name);
	}
    }
    
    private int getCreditsRemaining(Account account) throws Exception {
	sleepForMillis(500);//500ms
	return (int) Math.random()*100;
    }
    
    private Account getAccountByUserId(int userid) {
	return new Account(userid);
    }
    
    /** Expects a number between -1 and 10 **/
    private class Account {
	private int userid;
	
	public Account(int userid) {
	    this.userid = userid;
	}
	public String getAccountName() {
	    switch(userid) {
    	    case -1: return "Anonymous";
    	    case 0: return "Starbucks, Inc";
    	    case 1: return "Sterling Freight";
    	    case 2: return "Walmart";
    	    case 3: return "Nike";
    	    case 4: return "Acme";
    	    case 5: return "Cambia Health";
    	    case 6: return "CitiBank";
    	    case 7: return "AutoParts Unlimited";
    	    case 8: return "LiveNation";
    	    default: return "Unknown";
	    }
	}
	
	public boolean isPlatinum() {
	    return userid % 2 == 0;
	}
    }
}