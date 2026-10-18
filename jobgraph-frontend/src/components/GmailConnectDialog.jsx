import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import './GmailConnectDialog.css'

export default function GmailConnectDialog({ open, replacing, email, onEmailChange, busy, error, onClose, onContinue }) {
  const dialogRef = useRef(null)
  const emailRef = useRef(null)

  useEffect(() => {
    if (!open || !dialogRef.current) return
    const dialog = dialogRef.current
    dialog.showModal()
    emailRef.current?.focus()
    return () => { if (dialog.open) dialog.close() }
  }, [open])

  if (!open) return null

  // A native dialog keeps keyboard focus inside the connection flow.
  return createPortal(<dialog ref={dialogRef} className="gmail-dialog" aria-labelledby="gmail-dialog-title" aria-describedby="gmail-dialog-description" onCancel={event => { event.preventDefault(); if (!busy) onClose() }}>
    <div className="gmail-dialog__top"><span className="eyebrow">CONNECT YOUR INBOX</span><button type="button" className="gmail-dialog__close" aria-label="Close Gmail connection" disabled={busy} onClick={onClose}>×</button></div>
    <div className="gmail-dialog__symbol" aria-hidden="true">✉</div>
    <h2 id="gmail-dialog-title">{replacing ? 'Choose your new Gmail.' : 'Which Gmail should we connect?'}</h2>
    <p id="gmail-dialog-description">Enter the address where your job emails arrive. It can be different from your JobGraph login.</p>
    <form onSubmit={event => { event.preventDefault(); onContinue(email.trim()) }}>
      <label htmlFor="gmail-connection-email">Gmail address</label>
      <input ref={emailRef} id="gmail-connection-email" type="email" autoComplete="email" placeholder="you@gmail.com" required maxLength={254} value={email} onChange={event => onEmailChange(event.target.value)} disabled={busy} />
      {error && <p className="gmail-dialog__error" role="alert">{error}</p>}
      <button className="button gmail-dialog__continue" type="submit" disabled={busy}>{busy ? 'Opening Google…' : 'Continue with Google'} <span aria-hidden="true">↗</span></button>
    </form>
    <div className="gmail-dialog__steps" aria-label="Connection steps"><span><b>01</b> Enter address</span><span><b>02</b> Verify with Google</span><span><b>03</b> Connected</span></div>
    <p className="gmail-dialog__note">Google may ask for a verification code on its own sign-in page. Enter it there, then approve read-only Gmail access.</p>
  </dialog>, document.body)
}
