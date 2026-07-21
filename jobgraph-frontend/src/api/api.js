const API_BASE = 'http://localhost:8080'

export async function connectGmail() {
    const res = await fetch(`${API_BASE}/emails/fetch`, { method: 'POST' })
    if (!res.ok) throw new Error('Gmail fetch request failed')
    return res.json().catch(() => null)
}

export async function uploadResume(resumeFile, additionalInfo) {
    const formData = new FormData()
    formData.append('resume', resumeFile)
    formData.append('additionalInfo', additionalInfo)

    const res = await fetch(`${API_BASE}/profile/upload`, {
        method: 'POST',
        body: formData,
    })
    if (!res.ok) throw new Error('Resume upload failed')
    return res.json()
}