import { useState } from 'react'
import { saveProfile } from '../api/api.js'
import './ProfilePreview.css'

function normalize(profile) {
  // Missing sections become empty inputs that users can fill in.
  return {
    fname: profile.fname || '', lname: profile.lname || '', email: profile.email || '',
    linkedinUrl: profile.linkedinUrl || '', codingHandles: profile.codingHandles || {},
    skills: profile.skills || {}, additionalSections: profile.additionalSections || {},
    experience: Array.isArray(profile.experience) ? profile.experience : [],
    projects: Array.isArray(profile.projects) ? profile.projects : [],
  }
}

function TextField({ label, value, onChange, type = 'text', placeholder = '' }) {
  return <label className="editor-field"><span>{label}</span><input type={type} value={value ?? ''} onChange={event => onChange(event.target.value)} placeholder={placeholder} /></label>
}

function RecordEditor({ title, description, data, arrayValues, onChange }) {
  // Skills, coding handles, and extra sections all have editable named groups.
  const entries = Object.entries(data || {})
  function update(index, key, value) {
    const next = entries.map(([oldKey, oldValue], itemIndex) => itemIndex === index ? [key, value] : [oldKey, oldValue])
    onChange(Object.fromEntries(next))
  }
  return <section className="detail-card editor-section">
    <div className="detail-card__heading"><div><span className="eyebrow">PROFILE DETAIL</span><h4>{title}</h4></div></div>
    <p className="editor-help">{description}</p>
    {entries.map(([key, value], index) => <div className="editor-record" key={index}>
      <TextField label="Label" value={key} onChange={nextKey => update(index, nextKey, value)} />
      <label className="editor-field"><span>{arrayValues ? 'Items (one per line)' : 'Value or link'}</span>{arrayValues
        ? <textarea rows="3" value={(value || []).join('\n')} onChange={event => update(index, key, event.target.value.split('\n').map(item => item.trim()).filter(Boolean))} />
        : <input value={value || ''} onChange={event => update(index, key, event.target.value)} />}</label>
      <button type="button" className="editor-remove" onClick={() => onChange(Object.fromEntries(entries.filter((_, itemIndex) => itemIndex !== index)))}>Remove</button>
    </div>)}
    <button type="button" className="editor-add" onClick={() => {
      let index = 1
      while (Object.hasOwn(data || {}, `new_${title.toLowerCase().replace(/\s/g, '_')}_${index}`)) index += 1
      onChange({ ...data, [`new_${title.toLowerCase().replace(/\s/g, '_')}_${index}`]: arrayValues ? [] : '' })
    }}>+ Add {title.toLowerCase()}</button>
  </section>
}

function BulletsEditor({ bullets = [], onChange }) {
  // The same highlight editor is used for experience and projects.
  const items = Array.isArray(bullets) ? bullets : []
  function update(index, field, value) { onChange(items.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item)) }
  return <div className="editor-bullets"><span className="editor-subheading">Highlights</span>
    {items.map((bullet, index) => <div className="editor-bullet" key={index}>
      <label className="editor-field"><span>What you did</span><textarea rows="2" value={bullet.text || ''} onChange={event => update(index, 'text', event.target.value)} /></label>
      <TextField label="Tags (comma separated)" value={(bullet.tags || []).join(', ')} onChange={value => update(index, 'tags', value.split(',').map(tag => tag.trim()).filter(Boolean))} />
      <button type="button" className="editor-remove" onClick={() => onChange(items.filter((_, itemIndex) => itemIndex !== index))}>Remove highlight</button>
    </div>)}
    <button type="button" className="editor-add" onClick={() => onChange([...items, { text: '', tags: [] }])}>+ Add highlight</button>
  </div>
}

export default function ProfilePreview({ profile, onStartOver, onSaved, initiallySaved = false }) {
  // Keep edits local until Save succeeds, so the parsed draft stays reviewable.
  const [draft, setDraft] = useState(() => normalize(profile))
  const [saved, setSaved] = useState(initiallySaved)
  const [dirty, setDirty] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  function change(next) { setDraft(next); setDirty(true); setNotice('') }
  function basic(field, value) { change({ ...draft, [field]: value }) }
  function list(field, index, item) {
    change({ ...draft, [field]: draft[field].map((value, itemIndex) => itemIndex === index ? item : value) })
  }
  function remove(field, index) { change({ ...draft, [field]: draft[field].filter((_, itemIndex) => itemIndex !== index) }) }

  async function handleSave(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      // The response contains the saved version, including all edited sections.
      const result = await saveProfile(draft)
      setDraft(normalize(result))
      setSaved(true)
      setDirty(false)
      setNotice('Profile saved. Opening your saved profile…')
      onSaved(result)
    } catch (requestError) { setError(requestError.message || 'Could not save your profile.') }
    finally { setBusy(false) }
  }

  return <form className="profile-preview" onSubmit={handleSave}>
    <div className="profile-preview__top"><span className="eyebrow">PROFILE STUDIO / 02</span><span className="profile-preview__badge">{saved ? '✳ SAVED PROFILE' : '✳ UNSAVED DRAFT'}</span></div>
    <div className="profile-preview__identity"><div className="profile-preview__avatar" aria-hidden="true">{(draft.fname?.[0] || 'Y').toUpperCase()}</div><div><p className="profile-preview__caption">Make it yours</p><h3>{[draft.fname, draft.lname].filter(Boolean).join(' ') || 'Your profile'}</h3><p>{draft.email || 'Add a contact email below'}</p></div></div>
    <p className="profile-preview__intro">Everything here is editable. Check the details, add what the resume missed, then save when it feels right.</p>

    <div className="profile-preview__details">
      <section className="detail-card editor-section"><div className="detail-card__heading"><div><span className="eyebrow">01 / THE ESSENTIALS</span><h4>About you</h4></div></div><div className="editor-grid">
        <TextField label="First name" value={draft.fname} onChange={value => basic('fname', value)} />
        <TextField label="Last name" value={draft.lname} onChange={value => basic('lname', value)} />
        <TextField label="Profile contact email" type="email" value={draft.email} onChange={value => basic('email', value)} placeholder="Can differ from login email" />
        <TextField label="LinkedIn URL" type="url" value={draft.linkedinUrl} onChange={value => basic('linkedinUrl', value)} />
      </div></section>
      <RecordEditor title="Coding handles" description="Add links or usernames for the places you share work." data={draft.codingHandles} onChange={value => basic('codingHandles', value)} />
      <RecordEditor title="Skills" description="Give each group a name and put one skill on each line." data={draft.skills} arrayValues onChange={value => basic('skills', value)} />
      <section className="detail-card editor-section"><div className="detail-card__heading"><div><span className="eyebrow">02 / THE JOURNEY</span><h4>Experience</h4></div></div>
        {draft.experience.map((item, index) => <div className="editor-item" key={index}><div className="editor-item__head"><strong>Role {index + 1}</strong><button type="button" className="editor-remove" onClick={() => remove('experience', index)}>Remove role</button></div><div className="editor-grid">
          <TextField label="Company" value={item.companyName} onChange={value => list('experience', index, { ...item, companyName: value })} />
          <TextField label="Role" value={item.role} onChange={value => list('experience', index, { ...item, role: value })} />
          <TextField label="Start date" type="date" value={item.startDate} onChange={value => list('experience', index, { ...item, startDate: value || null })} />
          <TextField label="End date (blank if current)" type="date" value={item.endDate} onChange={value => list('experience', index, { ...item, endDate: value || null })} />
        </div><BulletsEditor bullets={item.bullets} onChange={bullets => list('experience', index, { ...item, bullets })} /></div>)}
        <button type="button" className="editor-add" onClick={() => change({ ...draft, experience: [...draft.experience, { companyName: '', role: '', startDate: null, endDate: null, bullets: [] }] })}>+ Add experience</button>
      </section>
      <section className="detail-card editor-section"><div className="detail-card__heading"><div><span className="eyebrow">03 / THE WORK</span><h4>Projects</h4></div></div>
        {draft.projects.map((item, index) => <div className="editor-item" key={index}><div className="editor-item__head"><strong>Project {index + 1}</strong><button type="button" className="editor-remove" onClick={() => remove('projects', index)}>Remove project</button></div><div className="editor-grid">
          <TextField label="Project name" value={item.name} onChange={value => list('projects', index, { ...item, name: value })} />
          <label className="editor-field"><span>Description</span><textarea rows="3" value={item.description || ''} onChange={event => list('projects', index, { ...item, description: event.target.value })} /></label>
        </div><BulletsEditor bullets={item.bullets} onChange={bullets => list('projects', index, { ...item, bullets })} /></div>)}
        <button type="button" className="editor-add" onClick={() => change({ ...draft, projects: [...draft.projects, { name: '', description: '', bullets: [] }] })}>+ Add project</button>
      </section>
      <RecordEditor title="Additional sections" description="Achievements, certifications, publications, or anything else worth keeping. One item per line." data={draft.additionalSections} arrayValues onChange={value => basic('additionalSections', value)} />
    </div>
    <div className="profile-preview__actions"><button className="button button--accent" type="submit" disabled={busy || (saved && !dirty)}>{busy ? 'Saving…' : saved ? 'Save changes' : 'Save my profile'} <span aria-hidden="true">↗</span></button><button className="button button--quiet" type="button" onClick={() => { if (!dirty || window.confirm('Discard unsaved changes and upload another resume?')) onStartOver() }}>Upload another resume</button></div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {notice && <p className="save-success" role="status">{notice}</p>}
  </form>
}
