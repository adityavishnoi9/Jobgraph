import './FetchedEmails.css'

function receivedLabel(value) {
  if (!value) return 'Date unavailable'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export default function FetchedEmails({ ref, emails, loading, error }) {
  return <section ref={ref} className="fetched-emails" id="fetched-emails" aria-labelledby="fetched-emails-title">
    <div className="fetched-emails__top"><span className="eyebrow">YOUR CONNECTED INBOX</span><span className="fetched-emails__count">{emails.length} {emails.length === 1 ? 'MESSAGE' : 'MESSAGES'}</span></div>
    <h2 id="fetched-emails-title">Fetched Gmail <em>messages.</em></h2>
    <p className="fetched-emails__intro">Messages found in your connected Gmail account, shown newest first.</p>
    {loading ? <p className="fetched-emails__state" role="status">Loading your messages…</p>
      : error ? <p className="fetched-emails__state fetched-emails__state--error" role="alert">{error}</p>
        : emails.length ? <ol className="fetched-emails__list">{emails.map((email, index) => <li className="fetched-email" key={email.messageId || index}>
          <div className="fetched-email__top"><span className="fetched-email__number">{String(index + 1).padStart(2, '0')}</span><time dateTime={email.receivedAt || undefined}>{receivedLabel(email.receivedAt)}</time></div>
          <h3>{email.subject?.trim() || '(No subject)'}</h3>
          <p className="fetched-email__sender">From <strong>{email.from?.trim() || 'Unknown sender'}</strong></p>
          <p className="fetched-email__snippet">{email.snippet?.trim() || 'No preview available.'}</p>
          {email.body?.trim() && <details><summary>Read message</summary><div className="fetched-email__body">{email.body}</div></details>}
        </li>)}</ol> : <p className="fetched-emails__state">No messages here yet. Connect Gmail and select “Fetch job alerts” to bring them in.</p>}
  </section>
}
