import React, { useState } from 'react'
import './UploadResumeForm.css'

const UPLOAD_URL = 'http://localhost:8080/profile/upload'

export default function UploadResumeForm() {
    const [resumeFile, setResumeFile] = useState(null)
    const [additionalInfo, setAdditionalInfo] = useState('')
    const [status, setStatus] = useState(null) // null | 'uploading' | 'done' | 'error'
    const [result, setResult] = useState(null)

    async function handleSubmit(e) {
        e.preventDefault()
        if (!resumeFile) return

        setStatus('uploading')
        try {
            const formData = new FormData()
            formData.append('resume', resumeFile)
            formData.append('additionalInfo', additionalInfo)

            const response = await fetch(UPLOAD_URL, {
                method: 'POST',
                body: formData,
            })
            console.log(response)
            if (!response.ok) {
                const message = await response.text()
                throw new Error(message || 'Resume upload failed.')
            }

            const data = await response.json()
            setResult(data)
            setStatus('done')
        } catch (err) {
            setStatus('error')
        }
    }

    return (
        <form className="upload-form" onSubmit={handleSubmit}>
            <div className="upload-form__field">
                <label htmlFor="resume">Resume</label>
                <input
                    id="resume"
                    type="file"
                    accept=".pdf,.docx"
                    onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)}
                />
            </div>

            <div className="upload-form__field">
                <label htmlFor="additional-info">Anything else you want to add?</label>
                <textarea
                    id="additional-info"
                    rows={4}
                    placeholder="Skills, side projects, or context not on your resume…"
                    value={additionalInfo}
                    onChange={(e) => setAdditionalInfo(e.target.value)}
                />
            </div>

            <button type="submit" className="upload-form__submit" disabled={!resumeFile || status === 'uploading'}>
                {status === 'uploading' ? 'Uploading…' : 'Submit'}
            </button>

            {status === 'error' && (
                <p className="upload-form__error">Upload failed. Check the server is running and try again.</p>
            )}

            {status === 'done' && result && (
                <div className="upload-form__result">
                    Extracted text received ({result.resumeRawText?.length ?? 0} characters).
                </div>
            )}
        </form>
    )
}
