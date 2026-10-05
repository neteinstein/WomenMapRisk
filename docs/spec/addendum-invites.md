# Addendum: invite-only registration

Source: the product owner, at kickoff on 2026-10-04. This is not part of the original spec, and it is authoritative alongside it.

## Rules (as stated by the product owner)

1. The app is **invite only**. Registration requires a valid invite code.
2. **Only registered users can invite**, and only once their account is enabled. Enabled means email confirmed, invite redeemed, and not blocked.
3. Each user can invite **up to 5 people**.
4. Invites become available after **3 consecutive days of usage**, or **5 intermittent (non-consecutive) days** of usage.
5. When inviting, users are **reminded that the app is for women only**.

## Engineering interpretation (change here if the product owner disagrees)

- **"Usage day":** a distinct calendar day (Europe/Lisbon) on which a signed-in user opened the app. Stored as `(user_id, day)` only.
- **The 5-invite cap** is lifetime and counts issued invites that are not revoked. Revoking an unused invite gives the slot back. Expired, unused invites also give the slot back.
- **Invite codes:** 8 characters from an unambiguous alphabet (no 0/O/1/I/L), single use, valid for 30 days.
- **Moderators** have unlimited invites and no usage requirement, so the first users can be seeded.
- **Visitors** ("Explorar sem conta", Screen 1) can still browse the map without an invite.
- **Women-only reminder copy:**
  - PT: "Women Risk Map é só para mulheres. Convida apenas mulheres em quem confias."
  - EN: "Women Risk Map is for women only. Only invite women you trust."
