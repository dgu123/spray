package slingong.web.gae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newmainsoftech.aspectjutil.eventmanager.EventInfo;
import com.newmainsoftech.aspectjutil.eventmanager.EventInfo.EventerInfo;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;
import com.newmainsoftech.aspectjutil.eventmanager.label.OnEvent;
import com.newmainsoftech.aspectjutil.eventmanager.label.PreEvent;

/**
 * Event class just for logging & debugging purpose.
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@EventListener
class MethodCallLogEvent {
	protected static Logger logger = LoggerFactory.getLogger( MethodCallLogEvent.class);
	
	@PreEvent( value={ MethodCallLogEvent.class}, amongInstancesInThread=false)
	protected static void logPublicMethodEntrance( EventInfo eventInfo) {
		if ( logger.isDebugEnabled()) {
			EventerInfo eventerInfo = eventInfo.getEventerInfo();
			
			// get dump of arguments ----------------------------------------------------------
			Object[] argumentsObjArray = eventInfo.getArgs();
			StringBuilder argumentsStringBuilder = new StringBuilder();
			if ( argumentsObjArray.length > 0) {
				for( int argIndex = 0; argIndex < argumentsObjArray.length; argIndex++) {
					if ( argIndex > 0) {
						argumentsStringBuilder.append( ", ");
					}
					argumentsStringBuilder.append( argumentsObjArray[ argIndex]);
				} // for
			}
			else {
				argumentsStringBuilder.append( "None argument");
			}
			// --------------------------------------------------------------------------------
			
			String message = String.format(
					"Entering %1$s in thread-%2$d.%n%3$cArguments: %4$s",
					eventerInfo.getEventerMethod().toString(),
					eventInfo.getThreadId(),
					'\t',
					argumentsStringBuilder.toString()
					);
			logger.debug( message);
		}
	} // protected void logPublicMethodEntrance( EventInfo eventInfo)
	
	@OnEvent( value={ MethodCallLogEvent.class}, amongInstancesInThread=false)
	protected static void logPublicMethodExit( EventInfo eventInfo) {
		if ( logger.isDebugEnabled()) {
			EventerInfo eventerInfo = eventInfo.getEventerInfo();
			String message = String.format(
					"Exited %1$s in thread-%2$d.%n%3$cReturning object: %4$s",
					eventerInfo.getEventerMethod().toString(),
					eventInfo.getThreadId(),
					'\t',
					( (eventInfo.getResult() == null) 
							? ( "void".equals( eventerInfo.getEventerMethod().getReturnType().toString()) 
									? "void" : "null"
									) 
								: eventInfo.getResult().toString()
							)
					);
			logger.debug( message);
		}
	} // protected void logPublicMethodExit( EventInfo eventInfo)
}
