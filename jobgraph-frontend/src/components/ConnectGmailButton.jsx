import { useEffect, useState } from 'react'
import { disconnectGmail, fetchEmails, getGmailStatus, startGmailConnection } from '../api/api.js'
import GmailConnectDialog from './GmailConnectDialog.jsx'

export default function ConnectGmailButton({ onFetched, onDisconnected, showFetch = false, showDisconnect = false }) {
  // Google returns to the inbox page with a short result in the URL.
  const oauthResult = new URLSearchParams(window.location.search).get('gmail')
  const [connection, setConnection] = useState(null)
  const [busy, setBusy] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const [dialogEmail, setDialogEmail] = useState('')
  const [dialogError, setDialogError] = useState('')
  const [error, setError] = useState(() => ({
    cancelled: 'Gmail connection was cancelled. You can try again.',
    'invalid-state': 'Your connection session expired. Please try again.',
    'email-mismatch': 'The Google account did not match the address you entered. Try again with the same address on both screens.',
    error: 'Google could not connect this Gmail account. Please try again.',
  })[oauthResult] || '')
  const [notice, setNotice] = useState(() => oauthResult === 'connected' ? 'Gmail connected successfully.' : '')

  useEffect(() => {
    let active = true
    // Read the connected Gmail address from the backend, not from a typed value.
    getGmailStatus().then(status => { if (active) setConnection(status) })
      .catch(requestError => { if (active) setError(requestError.message) })
    return () => { active = false }
  }, [])

  function openDialog() {
    setDialogEmail(connection?.email || '')
    setDialogError('')
    setDialogOpen(true)
    setError('')
  }

  async function connect(email) {
    setBusy('connecting'); setDialogError(''); setNotice('')
    try {
      // Google handles sign-in and any verification code before granting Gmail access.
      const { url } = await startGmailConnection(email)
      window.location.assign(url)
    } catch (requestError) { setDialogError(requestError.message || 'Could not start Gmail connection.'); setBusy('') }
  }

  async function disconnect() {
    setBusy('disconnecting'); setError(''); setNotice('')
    try { await disconnectGmail(); setConnection({ connected: false, email: null }); onDisconnected?.(); setNotice('Gmail disconnected.') }
    catch (requestError) { setError(requestError.message || 'Could not disconnect Gmail.') }
    finally { setBusy('') }
  }

  async function fetchAlerts() {
    setBusy('fetching'); setError(''); setNotice('')
    try {
      const result = await fetchEmails()
      // The fetch endpoint returns a count; the stack endpoint holds the messages.
      await onFetched?.()
      setNotice(result.fetched ? `Fetched ${result.fetched} Gmail message${result.fetched === 1 ? '' : 's'}.` : 'No new matching messages found.')
    }
    catch (requestError) { setError(requestError.message || 'Could not fetch messages. Reconnect Gmail if access was revoked.') }
    finally { setBusy('') }
  }

  return <div className="inbox-action">
    {connection?.connected && <p className="inbox-action__address">Connected: <strong>{connection.email}</strong></p>}
    <button className="button button--light" type="button" onClick={openDialog} disabled={Boolean(busy) || connection === null}>{connection?.connected ? 'Change Gmail account' : 'Connect Gmail'} <span aria-hidden="true">↗</span></button>
    {connection?.connected && (showFetch || showDisconnect) && <div className="inbox-action__secondary">{showFetch && <button type="button" onClick={fetchAlerts} disabled={Boolean(busy)}>{busy === 'fetching' ? 'Fetching…' : 'Fetch job alerts'}</button>}{showDisconnect && <button type="button" onClick={disconnect} disabled={Boolean(busy)}>{busy === 'disconnecting' ? 'Disconnecting…' : 'Disconnect'}</button>}</div>}
    {error && <p className="inbox-action__error" role="alert">{error}</p>}
    {notice && <p className="inbox-action__notice" role="status">{notice}</p>}
    <GmailConnectDialog open={dialogOpen} replacing={Boolean(connection?.connected)} email={dialogEmail} onEmailChange={setDialogEmail} busy={busy === 'connecting'} error={dialogError} onClose={() => setDialogOpen(false)} onContinue={connect} />
  </div>
}
