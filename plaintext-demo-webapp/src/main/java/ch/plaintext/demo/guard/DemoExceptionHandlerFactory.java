package ch.plaintext.demo.guard;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;

import java.util.Iterator;

/** Shows a refusal of the demo guard as a message on the page instead of an error. */
public class DemoExceptionHandlerFactory extends ExceptionHandlerFactory {

    public DemoExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
        super(wrapped);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new Handler(getWrapped().getExceptionHandler());
    }

    static final class Handler extends ExceptionHandlerWrapper {

        Handler(ExceptionHandler wrapped) {
            super(wrapped);
        }

        @Override
        public void handle() {
            Iterator<ExceptionQueuedEvent> events = getUnhandledExceptionQueuedEvents().iterator();
            while (events.hasNext()) {
                String refusal = refusal(events.next().getContext().getException());
                if (refusal != null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Public demo", refusal));
                    events.remove();
                }
            }
            getWrapped().handle();
        }
    }

    /** The guard's message somewhere in the cause chain, otherwise null. */
    static String refusal(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof DemoWriteGuard.DemoReadOnlyException) {
                return c.getMessage();
            }
        }
        return null;
    }
}
