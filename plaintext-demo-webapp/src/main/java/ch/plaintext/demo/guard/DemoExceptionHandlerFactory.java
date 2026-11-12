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
                DemoWriteGuard.DemoReadOnlyException refusal = refusal(events.next().getContext().getException());
                if (refusal != null) {
                    FacesContext ctx = FacesContext.getCurrentInstance();
                    ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, t(ctx, "Öffentliche Demo"),
                            t(ctx, DemoWriteGuard.DemoReadOnlyException.KEY) + ": " + refusal.getDetail() + ". "
                                    + t(ctx, DemoWriteGuard.DemoReadOnlyException.HINT)));
                    events.remove();
                }
            }
            getWrapped().handle();
        }
    }

    /** The guard's refusal somewhere in the cause chain, otherwise null. */
    static DemoWriteGuard.DemoReadOnlyException refusal(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            if (c instanceof DemoWriteGuard.DemoReadOnlyException refusal) {
                return refusal;
            }
        }
        return null;
    }

    /** root's i18n in the user's language; the German text if it is not available. */
    static String t(FacesContext ctx, String german) {
        try {
            Object i18n = ctx.getApplication().evaluateExpressionGet(ctx, "#{i18n}", Object.class);
            if (i18n instanceof ch.plaintext.i18n.web.I18nEL el) {
                return el.t(german);
            }
        } catch (RuntimeException e) {
            // fall back to German
        }
        return german;
    }
}
