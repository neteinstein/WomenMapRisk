#!/usr/bin/env python3
"""
Single source for all user-facing strings (EN default + PT).
Run:  python3 core/designsystem/strings.py   -> regenerates composeResources/values{,-pt}/strings.xml
Placeholders MUST be positional (%1$s, %1$d): Compose resources ignore bare %d/%s.
PT copy marked [spec] is VERBATIM from docs/spec/especificacao-funcional-v1.md, so do not reword it.
See .claude/skills/spec-copy-sync.
"""
import os
from xml.sax.saxutils import escape

S = [
# key, EN, PT
("app_name", "Women Risk Map", "Women Risk Map"),
# Common
("ok", "OK", "OK"), ("cancel", "Cancel", "Cancelar"), ("close", "Close", "Fechar"), ("retry", "Try again", "Tentar novamente"),
("save", "Save", "Guardar"), ("delete", "Delete", "Apagar"), ("edit", "Edit", "Editar"), ("back", "Back", "Voltar"),
("undo", "Undo", "Anular"), ("loading", "Loading…", "A carregar…"), ("continue_", "Continue", "Continuar"),
("just_now", "just now", "agora mesmo"),
# Welcome [spec Ecrã 1]
("welcome_tagline", "A map made by women, for women travelling alone. Know which streets to avoid.", "Um mapa feito por mulheres, para mulheres que viajam sozinhas. Sabe que ruas evitar."),
("welcome_create_account", "Create account", "Criar conta"),  # [spec]
("welcome_explore", "Explore without an account", "Explorar sem conta"),  # [spec]
("welcome_privacy", "Your reports are always anonymous.", "Os teus reportes são sempre anónimos."),  # [spec]
("welcome_have_account", "I already have an account", "Já tenho conta"),
("welcome_invite_only", "Women Risk Map is invite-only.", "A Women Risk Map funciona apenas por convite."),
# Auth [spec Ecrã 2]
("auth_title_signup", "Create account", "Criar conta"),
("auth_title_login", "Sign in", "Entrar"),
("auth_email", "Email", "Email"),
("auth_password", "Password", "Palavra-passe"),
("auth_password_hint", "At least 8 characters, with letters and numbers", "Pelo menos 8 caracteres, com letras e números"),
("auth_show_password", "Show password", "Mostrar palavra-passe"),
("auth_country", "Country", "País"),
("auth_invite_code", "Invite code", "Código de convite"),
("auth_invite_code_hint", "e.g. ABCD-EFGH", "ex.: ABCD-EFGH"),
("auth_accept_terms", "I accept the terms and the privacy policy", "Aceito os termos e a política de privacidade"),  # [spec]
("auth_declare_woman", "I declare that I am a woman", "Declaro que sou mulher"),
("auth_women_only_notice", "Women Risk Map is for women only. By creating an account you declare that you are a woman.", "A Women Risk Map é só para mulheres. Ao criar conta, declaras que és mulher."),
("auth_signup_button", "Create account", "Criar conta"),
("auth_login_button", "Sign in", "Entrar"),
("auth_google", "Continue with Google", "Continuar com Google"),
("auth_or", "or", "ou"),
("auth_switch_to_login", "Already have an account? Sign in", "Já tens conta? Entrar"),
("auth_switch_to_signup", "No account yet? Create one", "Não tens conta? Criar conta"),
("auth_view_terms", "Read the terms and privacy policy", "Ler os termos e a política de privacidade"),
("auth_check_email_title", "Confirm your email", "Confirma o teu email"),
("auth_check_email_body", "We sent a link to %1$s. Confirm your email before you can report.", "Enviámos um link para %1$s. Confirma o email antes de poderes reportar."),
("auth_resend", "Resend email", "Reenviar email"),
("auth_resent", "Email sent again.", "Email reenviado."),
("auth_confirmed_continue", "I have confirmed", "Já confirmei"),
# Errors [spec Ecrã 2: mensagens claras, em português]
("error_email_registered", "This email is already registered.", "Este email já está registado."),
("error_weak_password", "The password is too weak. Use at least 8 characters, with letters and numbers.", "A palavra-passe é fraca. Usa pelo menos 8 caracteres, com letras e números."),
("error_email_not_confirmed", "You haven’t confirmed your email yet. Check your inbox.", "Ainda não confirmaste o teu email. Verifica a tua caixa de entrada."),
("error_invalid_credentials", "Wrong email or password.", "Email ou palavra-passe incorretos."),
("error_invalid_invite", "Invalid or expired invite code.", "Código de convite inválido ou expirado."),
("error_email_invalid", "Enter a valid email.", "Introduz um email válido."),
("error_terms_required", "You must accept the terms and the privacy policy.", "Tens de aceitar os termos e a política de privacidade."),
("error_declare_required", "Women Risk Map is for women only.", "A Women Risk Map é só para mulheres."),
("error_network", "No internet connection. Try again.", "Sem ligação à internet. Tenta novamente."),
("error_generic", "Something went wrong. Try again.", "Algo correu mal. Tenta novamente."),
("error_daily_limit", "You reached the maximum of 5 reports per day.", "Atingiste o máximo de 5 reportes por dia."),
("error_duplicate", "You already reported this place today.", "Já reportaste este sítio hoje."),
("error_edit_window", "Reports can only be edited or deleted within 24 hours.", "Só podes editar ou apagar um reporte durante 24 horas."),
("error_already_confirmed", "You already confirmed this report.", "Já confirmaste este reporte."),
("error_cannot_confirm_own", "You can’t confirm your own report.", "Não podes confirmar o teu próprio reporte."),
("error_invites_locked", "Invites are not available yet.", "Os convites ainda não estão disponíveis."),
("error_invite_limit", "You have used all 5 invites.", "Já usaste os teus 5 convites."),
("error_not_allowed", "You don’t have permission to do this.", "Não tens permissão para fazer isto."),
("error_account_blocked", "Your account is blocked.", "A tua conta está bloqueada."),
("error_description_too_long", "The description can have at most 300 characters.", "A descrição pode ter no máximo 300 caracteres."),
# Invite gate (pending_invite)
("invite_gate_title", "Enter your invite code", "Introduz o teu código de convite"),
("invite_gate_body", "Women Risk Map is invite-only. Ask a woman who already uses the app for a code.", "A Women Risk Map funciona apenas por convite. Pede um código a uma mulher que já use a app."),
("invite_gate_submit", "Activate account", "Ativar conta"),
("invite_gate_later", "Explore without an account for now", "Explorar sem conta por agora"),
# Navigation [spec Ecrã 3]
("nav_map", "Map", "Mapa"), ("nav_saved", "Saved", "Guardados"), ("nav_profile", "Profile", "Perfil"),
# Map [spec Ecrã 3]
("map_search_hint", "Search street, address or place", "Pesquisar rua, morada ou local"),
("map_filters", "Filters", "Filtros"),
("map_report", "Report", "Reportar"),  # [spec]
("map_center_me", "Center on me", "Centrar em mim"),  # [spec]
("map_empty", "No information in this area yet. Be the first to contribute.", "Ainda sem informação nesta zona. Sê a primeira a contribuir."),  # [spec]
("map_stale", "Outdated data", "Dados desatualizados"),  # [spec §7]
("map_offline_report_disabled", "Offline: reporting is unavailable.", "Sem internet: não é possível reportar."),
("map_filters_active", "Filters on", "Filtros ativos"),
("map_no_results", "No results", "Sem resultados"),
("map_location_denied", "Location is off. Showing %1$s.", "Localização desativada. A mostrar %1$s."),
("legend_title", "Legend", "Legenda"),
("legend_green", "No reports", "Sem reportes"),
("legend_yellow", "Some reports", "Alguns reportes"),
("legend_red", "Many reports", "Muitos reportes"),
("risk_green_cd", "Safe zone, no reports", "Zona sem reportes"),
("risk_yellow_cd", "Caution, some reports", "Atenção, alguns reportes"),
("risk_red_cd", "Danger, many reports", "Perigo, muitos reportes"),
("visitor_prompt_title", "Create an account to contribute", "Cria uma conta para contribuir"),
("visitor_prompt_body", "Visitors can see the map and alerts. To report, confirm or save zones you need an account (invite-only).", "Visitantes podem ver o mapa e os alertas. Para reportar, confirmar ou guardar zonas precisas de uma conta (só por convite)."),
("visitor_prompt_signup", "Create account", "Criar conta"),
("visitor_prompt_login", "Sign in", "Entrar"),
("email_unconfirmed_banner", "Confirm your email to start reporting.", "Confirma o teu email para poderes reportar."),
# Zone detail [spec Ecrã 4]
("zone_unknown_name", "Approximate area", "Zona aproximada"),
("zone_also_felt", "I felt this too", "Também senti isto"),  # [spec]
("zone_flag", "Report as inappropriate", "Denunciar reporte"),  # [spec]
("zone_save", "Save zone", "Guardar zona"),  # [spec]
("zone_saved", "Zone saved", "Zona guardada"),
("zone_report_here", "Report here", "Reportar aqui"),  # [spec]
("zone_confirmed", "Thank you for confirming.", "Obrigada por confirmares."),
("zone_more_actions", "More actions", "Mais ações"),
("zone_yours", "Your report", "O teu reporte"),
("flag_title", "Report as inappropriate", "Denunciar reporte"),
("flag_reason_false_information", "False information", "Informação falsa"),
("flag_reason_personal_data", "Contains personal data", "Contém dados pessoais"),
("flag_reason_offensive", "Offensive language", "Linguagem ofensiva"),
("flag_reason_spam", "Spam", "Spam"),
("flag_reason_other", "Other", "Outro"),
("flag_submit", "Send", "Denunciar"),
("flag_done", "Thank you. Moderation will review this report.", "Obrigada. A moderação vai rever este reporte."),
# Report types / when / period [spec Ecrã 5]
("type_verbal_harassment", "Verbal harassment", "Assédio verbal"),
("type_followed", "Followed", "Seguida"),
("type_poorly_lit", "Poorly lit street", "Rua mal iluminada"),
("type_deserted", "Deserted area", "Zona deserta"),
("type_robbery", "Robbery", "Roubo"),
("type_assault", "Assault", "Agressão"),
("type_other", "Other", "Outro"),
("when_now", "Now", "Agora"), ("when_today", "Today", "Hoje"), ("when_this_week", "This week", "Esta semana"), ("when_earlier", "Earlier", "Há mais tempo"),
("period_day", "Day", "Dia"), ("period_night", "Night", "Noite"), ("period_both", "Both", "Ambos"),
("status_pending", "Pending", "Pendente"), ("status_published", "Published", "Publicado"), ("status_hidden", "Under review", "Em revisão"), ("status_removed", "Removed", "Removido"),
# Report form [spec Ecrã 5]
("report_title", "Make a report", "Fazer um reporte"),
("report_edit_title", "Edit report", "Editar reporte"),
("report_location", "Location", "Localização"),
("report_location_hint", "Move the map to place the pin", "Move o mapa para ajustar o pino"),
("report_location_approx", "The location is rounded to an approximate area (~50 m).", "A localização é arredondada para uma zona aproximada (~50 m)."),
("report_search_street", "Search a street", "Pesquisar uma rua"),
("report_type", "Type of situation", "Tipo de situação"),
("report_when", "When did it happen", "Quando aconteceu"),
("report_period", "Time of day", "Período do dia"),
("report_description", "Description (optional)", "Descrição (opcional)"),
("report_description_warning", "Don’t include names, licence plates or data that identifies people.", "Não incluas nomes, matrículas nem dados que identifiquem pessoas."),  # [spec]
("report_chars", "%1$d/%2$d", "%1$d/%2$d"),
("report_establishment", "It’s about an establishment (bar, shop, hotel)", "É sobre um estabelecimento (bar, loja, hotel)"),
("report_establishment_hint", "Reports about establishments are reviewed before publishing.", "Reportes sobre estabelecimentos são revistos antes de publicar."),
("report_send", "Send", "Enviar"),  # [spec]
("report_thanks", "Thank you, your report helps other women.", "Obrigada, o teu reporte ajuda outras mulheres."),  # [spec]
("report_pending_review", "Your report will be reviewed before it is published.", "O teu reporte vai ser revisto antes de ser publicado."),
("report_edit_until", "You can edit or delete it for 24 hours.", "Podes editar ou apagar durante 24 horas."),
("report_guard_license_plate", "It looks like you included a licence plate.", "Parece que incluíste uma matrícula."),
("report_guard_phone_number", "It looks like you included a phone number.", "Parece que incluíste um número de telefone."),
("report_guard_email", "It looks like you included an email address.", "Parece que incluíste um email."),
("report_guard_url", "It looks like you included a link.", "Parece que incluíste um link."),
("report_delete_confirm", "Delete this report?", "Apagar este reporte?"),
("report_deleted", "Report deleted.", "Reporte apagado."),
("report_done", "Back to map", "Voltar ao mapa"),
# Support after assault [spec Ecrã 5]
("support_title", "You are not alone", "Não estás sozinha"),
("support_body", "What happened is not your fault. If you are in danger, call 112 now. You can also talk to a support line, free and confidential.", "O que aconteceu não é culpa tua. Se estiveres em perigo, liga já para o 112. Podes também falar com uma linha de apoio, de forma gratuita e confidencial."),
("emergency_emergency", "Emergency", "Emergência"),
("emergency_victim_support", "Victim support (APAV)", "Apoio à Vítima (APAV)"),
("emergency_domestic_violence", "Domestic violence line", "Linha de violência doméstica"),
("support_call", "Call %1$s", "Ligar %1$s"),
# Filters [spec Ecrã 6]
("filters_title", "Filters", "Filtros"),
("filters_type", "Type of situation", "Tipo de situação"),
("filters_period", "Period", "Período"),
("filter_last_week", "Last week", "Última semana"),
("filter_last_month", "Last month", "Último mês"),
("filter_last_6_months", "Last 6 months", "Últimos 6 meses"),
("filter_last_12_months", "Last 12 months", "Últimos 12 meses"),
("filters_day_period", "Time of day", "Período do dia"),
("filters_apply", "Apply", "Aplicar"),  # [spec]
("filters_clear", "Clear filters", "Limpar filtros"),  # [spec]
# Saved [spec Ecrã 7]
("saved_title", "Saved", "Guardados"),
("saved_empty", "You haven’t saved any zones yet. Tap a zone on the map and choose “Save zone”.", "Ainda não guardaste nenhuma zona. Toca numa zona do mapa e escolhe “Guardar zona”."),
("saved_deleted", "Zone removed", "Zona removida"),
("saved_visitor", "Create an account to save zones.", "Cria uma conta para guardares zonas."),
("saved_swipe_hint", "Swipe to delete", "Desliza para apagar"),
# Invites (addendum)
("invites_title", "Invite women", "Convidar mulheres"),
("invites_locked_title", "Invites are not available yet", "Os convites ainda não estão disponíveis"),
("invites_locked_body", "Invites unlock after you use the app 3 days in a row, or 5 days in total.", "Os convites ficam disponíveis depois de usares a app 3 dias seguidos, ou 5 dias no total."),
("invites_progress_streak", "%1$d/3 days in a row", "%1$d/3 dias seguidos"),
("invites_progress_days", "%1$d/5 days", "%1$d/5 dias"),
("invites_used", "%1$d/5 invites used", "%1$d/5 convites usados"),
("invites_unlimited", "Unlimited invites (moderator)", "Convites ilimitados (moderadora)"),
("invites_create", "Create invite", "Criar convite"),
("invites_women_only", "Women Risk Map is for women only. Only invite women you trust.", "A Women Risk Map é só para mulheres. Convida apenas mulheres em quem confias."),
("invites_confirm_checkbox", "I confirm I’m inviting a woman I trust", "Confirmo que vou convidar uma mulher em quem confio"),
("invites_share_message", "I’m inviting you to Women Risk Map, a safety map made by women. Your code: %1$s\n%2$s", "Convido-te para a Women Risk Map, um mapa de segurança feito por mulheres. O teu código: %1$s\n%2$s"),
("invites_share", "Share", "Partilhar"),
("invites_copy", "Copy code", "Copiar código"),
("invites_copied", "Code copied", "Código copiado"),
("invites_revoke", "Revoke", "Revogar"),
("invites_expires", "Expires %1$s", "Expira a %1$s"),
("invites_none", "You haven’t created any invites yet.", "Ainda não criaste convites."),
("invites_limit_reached", "You have used all 5 invites.", "Já usaste os teus 5 convites."),
("invites_not_enabled", "Confirm your email to invite other women.", "Confirma o teu email para poderes convidar outras mulheres."),
("invite_state_pending", "Unused", "Por usar"), ("invite_state_used", "Used", "Usado"), ("invite_state_expired", "Expired", "Expirado"), ("invite_state_revoked", "Revoked", "Revogado"),
# Profile [spec Ecrã 8]
("profile_title", "Profile", "Perfil"),
("profile_email", "Email", "Email"),
("profile_pseudonym", "Pseudonym (optional)", "Pseudónimo (opcional)"),
("profile_country", "Country", "País"),
("profile_my_reports", "My reports", "Os meus reportes"),  # [spec]
("profile_no_reports", "You haven’t made any reports yet.", "Ainda não fizeste reportes."),
("profile_settings", "Settings and privacy", "Definições e privacidade"),
("profile_help", "Help", "Ajuda"),
("profile_terms", "Terms and privacy", "Termos e privacidade"),
("profile_logout", "Sign out", "Terminar sessão"),
("profile_invite", "Invite women", "Convidar mulheres"),
("profile_moderation", "Moderation panel", "Painel de moderação"),
("profile_saved", "Profile updated", "Perfil atualizado"),
("profile_visitor_title", "You are exploring without an account", "Estás a explorar sem conta"),
("profile_visitor_body", "Create an account (invite-only) to report, confirm reports and save zones.", "Cria uma conta (só por convite) para reportares, confirmares reportes e guardares zonas."),
# Settings [spec Ecrã 9]
("settings_title", "Settings and privacy", "Definições e privacidade"),
("settings_location", "Location permission", "Permissão de localização"),
("settings_location_why", "We only use your location to center the map and to suggest where to make a report. Your exact position is never shown to anyone.", "Usamos a tua localização apenas para centrar o mapa e para sugerir onde fazer um reporte. Nunca mostramos a tua posição exata a ninguém."),
("settings_location_granted", "Allowed", "Permitida"),
("settings_location_denied", "Not allowed", "Não permitida"),
("settings_location_request", "Allow", "Permitir"),
("settings_history", "Location history", "Histórico de localização"),
("settings_history_body", "When on, only the last approximate zone you were in is kept, on this device only, to open the map there. Off by default.", "Quando ativo, guardamos apenas a última zona aproximada onde estiveste, só neste dispositivo, para abrir o mapa aí. Desativado por defeito."),
("settings_language", "Language", "Idioma"),
("settings_language_body", "The app follows your device language (Português or English). You can change it per app in the system settings.", "A app segue o idioma do dispositivo (Português ou English). Podes mudá-lo só para esta app nas definições do sistema."),
("settings_language_open", "Open system settings", "Abrir definições do sistema"),
("settings_export", "Export my data", "Exportar os meus dados"),  # [spec]
("settings_export_body", "Get a copy of everything we store about you.", "Recebe uma cópia de tudo o que guardamos sobre ti."),
("settings_delete", "Delete my account and all my data", "Apagar a minha conta e todos os meus dados"),  # [spec]
("settings_delete_confirm_title", "Delete account?", "Apagar conta?"),
("settings_delete_confirm_body", "This is permanent. Your personal data is deleted. Your reports stay on the map with no link to you.", "Esta ação é definitiva. Os teus dados pessoais são apagados. Os teus reportes ficam no mapa, sem qualquer ligação a ti."),
("settings_delete_confirm", "Delete permanently", "Apagar definitivamente"),
("settings_deleted", "Your account was deleted.", "A tua conta foi apagada."),
("settings_theme_note", "Light and dark themes follow your device.", "O tema claro e escuro segue o dispositivo."),
# Help & terms
("help_title", "Help", "Ajuda"),
("help_body", "• Reports are anonymous: other women never see who reported.\n• Locations are rounded to an approximate area (~50 m).\n• Colours: green = no reports, yellow = some, red = many. Each colour also has a symbol.\n• You can make up to 5 reports per day and edit or delete each one for 24 hours.\n• Tap “I felt this too” to confirm a report, or “Report as inappropriate” if it breaks the rules.\n• In an emergency, always call 112.", "• Os reportes são anónimos: as outras mulheres nunca veem quem reportou.\n• A localização é arredondada para uma zona aproximada (~50 m).\n• Cores: verde = sem reportes, amarelo = alguns, vermelho = muitos. Cada cor tem também um símbolo.\n• Podes fazer até 5 reportes por dia e editar ou apagar cada um durante 24 horas.\n• Toca em “Também senti isto” para confirmar um reporte, ou em “Denunciar reporte” se não cumprir as regras.\n• Em caso de emergência, liga sempre 112."),
("terms_title", "Terms and privacy", "Termos e privacidade"),
("terms_body", "DRAFT, pending legal review.\n\nWomen Risk Map is a community map for women. By using it you agree to:\n• Only report situations you experienced or witnessed.\n• Never include names, licence plates, photos of people or insults.\n• Be a woman, and invite only women you trust.\n\nPrivacy: reports are anonymous to other users; locations are rounded to ~50 m; we store your email, optional pseudonym, country and the days you used the app (for invites). You can export or delete all your data at any time in Settings.", "RASCUNHO, sujeito a revisão jurídica.\n\nA Women Risk Map é um mapa comunitário para mulheres. Ao usá-la, aceitas:\n• Reportar apenas situações que viveste ou presenciaste.\n• Nunca incluir nomes, matrículas, fotografias de pessoas nem insultos.\n• Ser mulher, e convidar apenas mulheres em quem confias.\n\nPrivacidade: os reportes são anónimos para as outras utilizadoras; a localização é arredondada para ~50 m; guardamos o teu email, pseudónimo opcional, país e os dias em que usaste a app (para os convites). Podes exportar ou apagar todos os teus dados a qualquer momento nas Definições."),
# Countries
("country_PT", "Portugal", "Portugal"), ("country_ES", "Spain", "Espanha"), ("country_FR", "France", "França"),
("country_GB", "United Kingdom", "Reino Unido"), ("country_DE", "Germany", "Alemanha"), ("country_IT", "Italy", "Itália"),
("country_NL", "Netherlands", "Países Baixos"), ("country_BR", "Brazil", "Brasil"), ("country_US", "United States", "Estados Unidos"),
("country_XX", "Other", "Outro"),
# Moderation [spec Ecrã 10]
("moderation_title", "Moderation panel", "Painel de moderação"),
("moderation_new_reports", "New reports", "Reportes novos"),  # [spec]
("moderation_pending_flags", "Pending flags", "Denúncias pendentes"),  # [spec]
("moderation_pending_establishments", "Establishments to review", "Estabelecimentos por rever"),
("moderation_tab_flagged", "Flagged", "Sinalizados"),
("moderation_tab_establishments", "Establishments", "Estabelecimentos"),
("moderation_approve", "Approve", "Aprovar"),  # [spec]
("moderation_remove", "Remove", "Remover"),  # [spec]
("moderation_block", "Block user", "Bloquear utilizadora"),  # [spec]
("moderation_reason", "Reason", "Motivo"),
("moderation_reason_required", "Enter a reason.", "Indica o motivo."),
("moderation_empty", "Nothing to review.", "Nada por rever."),
("moderation_not_allowed", "Only moderators have access.", "Apenas moderadoras têm acesso."),
("moderation_inviter_chain", "Invite chain", "Cadeia de convites"),
("moderation_revoke_invites", "Revoke pending invites", "Revogar convites pendentes"),
("moderation_done", "Action recorded.", "Ação registada."),
("moderation_flag_reasons", "Flag reasons", "Motivos das denúncias"),
("moderation_refresh", "Refresh", "Atualizar"),
]

P = [
# key, EN(one, other), PT(one, other)
("zone_reports", ("%1$d report", "%1$d reports"), ("%1$d reporte", "%1$d reportes")),
("zone_confirmed_by", ("%1$d woman confirmed", "%1$d women confirmed"), ("%1$d mulher confirmou", "%1$d mulheres confirmaram")),
("report_confirmations", ("%1$d confirmation", "%1$d confirmations"), ("%1$d confirmação", "%1$d confirmações")),
("moderation_flags_count", ("%1$d flag", "%1$d flags"), ("%1$d denúncia", "%1$d denúncias")),
("time_minutes_ago", ("%1$d min ago", "%1$d min ago"), ("há %1$d min", "há %1$d min")),
("time_hours_ago", ("%1$d hour ago", "%1$d hours ago"), ("há %1$d hora", "há %1$d horas")),
("time_days_ago", ("%1$d day ago", "%1$d days ago"), ("há %1$d dia", "há %1$d dias")),
]

def esc(v):
    v = escape(v).replace("\n", "\\n")  # CMP resources understand \n; apostrophes/quotes need no escaping
    return v

def write(path, idx, pidx):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- GENERATED by core/designsystem/strings.py. Edit the script, not this file. -->', '<resources>']
    for row in S:
        lines.append(f'    <string name="{row[0]}">{esc(row[idx])}</string>')
    for key, en, pt in P:
        forms = (en, pt)[pidx]
        lines.append(f'    <plurals name="{key}">')
        lines.append(f'        <item quantity="one">{esc(forms[0])}</item>')
        lines.append(f'        <item quantity="other">{esc(forms[1])}</item>')
        lines.append('    </plurals>')
    lines.append('</resources>\n')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w', encoding='utf-8').write('\n'.join(lines))

keys = [r[0] for r in S] + [p[0] for p in P]
assert len(keys) == len(set(keys)), "duplicate keys: " + str({k for k in keys if keys.count(k) > 1})
base = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'src/commonMain/composeResources')
write(os.path.join(base, 'values/strings.xml'), 1, 0)
write(os.path.join(base, 'values-pt/strings.xml'), 2, 1)
print(f"wrote {len(S)} strings + {len(P)} plurals (EN, PT)")
