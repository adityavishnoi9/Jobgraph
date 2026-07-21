import React, { useState } from 'react'
import { connectGmail } from '../api/api.js'

export default function ConnectGmailButton() {
    const [status, setStatus] = useState(null) // null | 'connecting' | 'connected' | 'error'

    async function handleClick() {
        setStatus('connecting')
        try {
            await connectGmail()
            setStatus('connected')
        } catch (err) {
            setStatus('error')
        }
    }

    const label = {
        connecting: 'Connecting…',
        connected: 'Gmail connected',
        error: 'Retry connecting Gmail',
    }[status] ?? 'Connect Gmail'

    return (
        <>
            <button className="btn btn-primary" onClick={handleClick} disabled={status === 'connecting'}>
                {label}
            </button>
            {status === 'error' && (
                <p className="error-text">Couldn't reach the server. Check it's running, then try again.</p>
            )}
        </>
    )
}