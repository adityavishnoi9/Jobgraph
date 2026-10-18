import { useRef, useState } from 'react'
import { uploadResume } from '../api/api.js'
import ProfilePreview from './ProfilePreview.jsx'
import './UploadResumeForm.css'

const ACCEPTED_EXTENSIONS = ['pdf', 'docx']

function formatFileSize(bytes) {
  return bytes < 1024 * 1024
    ? Math.max(1, Math.round(bytes / 1024)) + ' KB'
    : (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

export default function UploadResumeForm({ initialProfile, onProfileSaved }) {
  const inputRef = useRef(null)
  const [resumeFile, setResumeFile] = useState(null)
  const [additionalInfo, setAdditionalInfo] = useState('')
  const [isDragging, setIsDragging] = useState(false)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  // A saved profile opens in the editor; otherwise this component shows upload.
  const [profile, setProfile] = useState(initialProfile || null)
  const [saved, setSaved] = useState(Boolean(initialProfile))

  function chooseFile(file) {
    if (!file) return
    const extension = file.name.split('.').pop()?.toLowerCase()
    if (!ACCEPTED_EXTENSIONS.includes(extension)) {
      setResumeFile(null)
      setError('Please choose a PDF or DOCX resume.')
      return
    }
    if (file.size === 0) {
      setResumeFile(null)
      setError('This file is empty. Please choose a resume with content.')
      return
    }
    setResumeFile(file)
    setError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!resumeFile || status === 'uploading') return
    setStatus('uploading')
    setError('')
    try {
      // Parsing creates a draft only. ProfilePreview saves it after review.
      const result = await uploadResume(resumeFile, additionalInfo)
      setProfile(result)
      setSaved(false)
      setStatus('done')
    } catch (requestError) {
      setError(requestError.message || 'Upload failed. Please try again.')
      setStatus('error')
    }
  }

  function startOver() {
    setProfile(null)
    setResumeFile(null)
    setAdditionalInfo('')
    setStatus('idle')
    setError('')
    if (inputRef.current) inputRef.current.value = ''
  }

  // Tell the workspace when Save succeeds so it can open the saved-profile page.
  if (profile) return <ProfilePreview profile={profile} initiallySaved={saved} onStartOver={startOver} onSaved={savedProfile => { setSaved(true); onProfileSaved(savedProfile) }} />

  return (
    <form className="upload-form" onSubmit={handleSubmit}>
      <div className="upload-form__header">
        <span className="eyebrow">PROFILE STUDIO / 01</span>
        <span className="upload-form__spark" aria-hidden="true">✳</span>
      </div>
      <h3>Start with your resume.</h3>
      <p className="upload-form__intro">We’ll read it and organize the details into a profile you can review.</p>
      {initialProfile && <button className="button button--quiet" type="button" onClick={() => { setProfile(initialProfile); setSaved(true) }}>Return to saved profile</button>}

      <input
        ref={inputRef}
        id="resume"
        className="visually-hidden"
        type="file"
        accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        onChange={(event) => chooseFile(event.target.files?.[0])}
      />
      <div
        className={'dropzone' + (isDragging ? ' dropzone--active' : '') + (resumeFile ? ' dropzone--selected' : '')}
        onDragEnter={(event) => { event.preventDefault(); setIsDragging(true) }}
        onDragOver={(event) => event.preventDefault()}
        onDragLeave={(event) => { event.preventDefault(); setIsDragging(false) }}
        onDrop={(event) => {
          event.preventDefault()
          setIsDragging(false)
          chooseFile(event.dataTransfer.files?.[0])
        }}
      >
        <div className="dropzone__icon" aria-hidden="true">{resumeFile ? '✓' : '↥'}</div>
        <strong>{resumeFile ? resumeFile.name : 'Drop your resume here'}</strong>
        <span>{resumeFile ? formatFileSize(resumeFile.size) + ' · Ready to upload' : 'PDF or DOCX · drag and drop or browse'}</span>
        <button type="button" className="dropzone__browse" onClick={() => inputRef.current?.click()}>
          {resumeFile ? 'Choose a different file' : 'Browse files'} <span aria-hidden="true">↗</span>
        </button>
      </div>

      <label className="upload-form__label" htmlFor="additional-info">Anything your resume leaves out? <span>Optional</span></label>
      <textarea
        id="additional-info"
        rows="4"
        placeholder="A side project, a new skill, or the kind of work you’re looking for…"
        value={additionalInfo}
        onChange={(event) => setAdditionalInfo(event.target.value)}
      />

      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="button button--accent upload-form__submit" type="submit" disabled={!resumeFile || status === 'uploading'}>
        {status === 'uploading' ? 'Building your profile…' : 'Build my profile'} <span aria-hidden="true">↗</span>
      </button>
      <p className="upload-form__footnote" aria-live="polite">
        {status === 'uploading' ? 'Reading your resume and gathering the details. This may take a moment.' : 'You’ll get to review the result before saving it.'}
      </p>
    </form>
  )
}
