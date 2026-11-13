package ch.plaintext.demo.h2;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * root's session log ({@code plaintext-admin-sessions}) writes each visit with a PostgreSQL upsert
 * ({@code INSERT … ON CONFLICT (session_id) DO UPDATE}) that H2 rejects, which logged an ERROR per
 * request. The demo keeps no session log instead: its pages are blocked in the public demo anyway,
 * and visitors' browsers are not stored. Ending sessions (HttpSessionRegistry) works without it.
 */
// ponytail: skipped, not translated — with DEMO_GUARD=false the sessions page stays empty; translate to MERGE if someone needs it.
@Aspect
@Component
public class H2SessionAudit {

    @Around("execution(* ch.plaintext.sessions.repository.UserSessionRepository.upsertSession(..))")
    public Object skipUpsert() {
        return null;
    }
}
