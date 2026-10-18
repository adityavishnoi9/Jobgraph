import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { getProfile } from '../api/api.js'
import ConnectGmailButton from './ConnectGmailButton.jsx'
import './SavedProfilePage.css'

function text(value) {
  return String(value ?? '').trim()
}

function entries(record) {
  return Object.entries(record || {}).filter(([label, value]) => text(label) && (Array.isArray(value) ? value.some(item => text(item)) : text(value)))
}

function webLink(value) {
  // Only turn web URLs into clickable links; handles and other text stay plain.
  try {
    const url = new URL(text(value))
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null
  } catch { return null }
}

function dateLabel(value) {
  if (!value) return 'Present'
  const [year, month] = String(value).split('-').map(Number)
  if (!year || !month || month > 12) return String(value)
  return new Intl.DateTimeFormat('en', { month: 'short', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(year, month - 1, 1)))
}

function Highlights({ bullets }) {
  const items = (bullets || []).filter(bullet => text(bullet?.text) || bullet?.tags?.some(tag => text(tag)))
  if (!items.length) return null
  return <ul className="saved-highlights">{items.map((bullet, index) => <li key={index}>
    {text(bullet.text) && <p>{bullet.text}</p>}
    {bullet.tags?.length > 0 && <div className="saved-tags">{bullet.tags.filter(tag => text(tag)).map((tag, tagIndex) => <span key={tagIndex}>{tag}</span>)}</div>}
  </li>)}</ul>
}

function SectionHeader({ id, number, title, count }) {
  return <div className="saved-section__header"><span className="saved-section__number">{number}</span><h2 id={id}>{title}</h2><span className="saved-section__count">{count}</span></div>
}

export default function SavedProfilePage() {
  const location = useLocation()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  // Always read the saved copy, so this page reflects what the server kept.
  useEffect(() => {
    let active = true
    getProfile().then(result => { if (active) { setProfile(result); setLoading(false) } })
      .catch(requestError => { if (active) { setError(requestError.message || 'Could not load your profile.'); setLoading(false) } })
    return () => { active = false }
  }, [reloadKey])

  if (loading) return <main className="saved-page saved-page--state"><p className="eyebrow">YOUR PROFILE</p><h1>Opening your profile…</h1></main>
  if (error) return <main className="saved-page saved-page--state"><p className="eyebrow">YOUR PROFILE</p><h1>We couldn’t load your profile.</h1><p role="alert">{error}</p><button className="button button--dark" type="button" onClick={() => { setLoading(true); setError(''); setReloadKey(key => key + 1) }}>Try again <span aria-hidden="true">↗</span></button></main>
  if (!profile) return <Navigate to="/workspace" replace />

  const name = [text(profile.fname), text(profile.lname)].filter(Boolean).join(' ') || 'Your profile'
  const skills = entries(profile.skills)
  const handles = entries(profile.codingHandles)
  const extraSections = entries(profile.additionalSections)
  const experience = Array.isArray(profile.experience) ? profile.experience : []
  const projects = Array.isArray(profile.projects) ? profile.projects : []
  const linkedIn = webLink(profile.linkedinUrl)

  return <main className="saved-page" id="saved-profile">
    {location.state?.justSaved && <div className="saved-notice" role="status"><span aria-hidden="true">✓</span><div><strong>Profile saved</strong><p>Your latest edits are ready. Connect Gmail whenever you’re ready.</p></div></div>}

    <section className="saved-hero" aria-labelledby="saved-title">
      <div className="saved-hero__copy"><span className="saved-hero__eyebrow">YOUR SPACE / PROFILE</span><div className="saved-hero__status"><span aria-hidden="true" /> Saved and ready</div><h1 id="saved-title">{name}<span className="saved-hero__period">.</span></h1><p>A clear place for everything you’ve done, and everything you’ll do next.</p><div className="saved-hero__actions"><Link className="button saved-hero__edit" to="/workspace">Edit profile <span aria-hidden="true">↗</span></Link><a href="#saved-details" className="saved-hero__jump">Explore profile <span aria-hidden="true">↓</span></a></div></div>
      <div className="saved-hero__art" aria-hidden="true"><span className="saved-hero__ring saved-hero__ring--one" /><span className="saved-hero__ring saved-hero__ring--two" /><span className="saved-hero__initial">{text(profile.fname).charAt(0).toUpperCase() || 'J'}</span><span className="saved-hero__spark">✳</span></div>
    </section>

    <div className="saved-overview" aria-label="Profile overview"><div><span className="saved-overview__number">{experience.length.toString().padStart(2, '0')}</span><span>Roles</span></div><div><span className="saved-overview__number">{projects.length.toString().padStart(2, '0')}</span><span>Projects</span></div><div><span className="saved-overview__number">{skills.reduce((count, [, items]) => count + items.length, 0).toString().padStart(2, '0')}</span><span>Skills</span></div><p>Your story, all in one place.</p></div>

    <div className="saved-layout" id="saved-details">
      <aside className="saved-sidebar" aria-label="Gmail connection and contact details">
        <section className="saved-gmail"><div className="saved-gmail__icon" aria-hidden="true">✉</div><span className="eyebrow">THE NEXT CONNECTION</span><h2>Bring your inbox <em>into the picture.</em></h2><p>Connect the Gmail account where your job alerts arrive. It can be different from your login email.</p><ConnectGmailButton /><Link className="saved-gmail__inbox-link" to="/inbox">Go to inbox <span aria-hidden="true">↗</span></Link></section>
        <section className="saved-contact"><span className="eyebrow">CONTACT & LINKS</span><h2>Find me here</h2>{text(profile.email) ? <div className="saved-contact__row"><span>Email</span><a href={`mailto:${text(profile.email)}`}>{profile.email}</a></div> : <p className="saved-empty">No contact email added yet.</p>}{text(profile.linkedinUrl) && <div className="saved-contact__row"><span>LinkedIn</span>{linkedIn ? <a href={linkedIn} target="_blank" rel="noopener noreferrer">View profile <span aria-hidden="true">↗</span></a> : <p>{profile.linkedinUrl}</p>}</div>}{handles.map(([label, value]) => <div className="saved-contact__row" key={label}><span>{label}</span>{webLink(value) ? <a href={webLink(value)} target="_blank" rel="noopener noreferrer">Open link <span aria-hidden="true">↗</span></a> : <p>{value}</p>}</div>)}</section>
      </aside>

      <div className="saved-main">
        <section className="saved-section" aria-labelledby="saved-skills"><SectionHeader id="saved-skills" number="01" title="Skills" count={skills.length} /><div className="saved-skills">{skills.length ? skills.map(([group, items]) => <div className="saved-skills__group" key={group}><h3>{group}</h3><div className="saved-skills__pills">{items.filter(item => text(item)).map((item, index) => <span key={index}>{item}</span>)}</div></div>) : <p className="saved-empty">Add skills while editing your profile.</p>}</div></section>

        <section className="saved-section" aria-labelledby="saved-experience"><SectionHeader id="saved-experience" number="02" title="Experience" count={experience.length} /><div className="saved-timeline">{experience.length ? experience.map((item, index) => <article className="saved-timeline__item" key={index}><span className="saved-timeline__dot" aria-hidden="true" /><div className="saved-timeline__top"><div><h3>{text(item.role) || 'Untitled role'}</h3><p className="saved-timeline__company">{text(item.companyName) || 'Company not added'}</p></div>{item.startDate && <span className="saved-timeline__date">{dateLabel(item.startDate)} — {dateLabel(item.endDate)}</span>}</div><Highlights bullets={item.bullets} /></article>) : <p className="saved-empty">Your work experience will appear here.</p>}</div></section>

        <section className="saved-section" aria-labelledby="saved-projects"><SectionHeader id="saved-projects" number="03" title="Projects" count={projects.length} /><div className="saved-projects">{projects.length ? projects.map((item, index) => <article className="saved-project" key={index}><div className="saved-project__index">{String(index + 1).padStart(2, '0')} / PROJECT</div><h3>{text(item.name) || 'Untitled project'}</h3>{text(item.description) && <p className="saved-project__description">{item.description}</p>}<Highlights bullets={item.bullets} /></article>) : <p className="saved-empty">Your projects will appear here.</p>}</div></section>

        {extraSections.length > 0 && <section className="saved-section" aria-labelledby="saved-more"><SectionHeader id="saved-more" number="04" title="More about me" count={extraSections.length} /><div className="saved-extra">{extraSections.map(([label, items]) => <div key={label}><h3>{label}</h3><ul>{items.filter(item => text(item)).map((item, index) => <li key={index}>{item}</li>)}</ul></div>)}</div></section>}
        <div className="saved-end"><span aria-hidden="true">✳</span><p>Keep your story current as you grow.</p><Link to="/workspace">Make an edit <span aria-hidden="true">↗</span></Link></div>
      </div>
    </div>
  </main>
}
