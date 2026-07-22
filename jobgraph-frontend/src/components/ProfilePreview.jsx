import './ProfilePreview.css'

export default function ProfilePreview({ profile }) {
    return (
        <section className="profile-preview">
            <p className="profile-preview__eyebrow">Resume uploaded</p>
            <h2>Extracted resume text</h2>

            <pre className="profile-preview__text">{profile.resumeRawText}</pre>

            {profile.additionalInfo && (
                <div className="profile-preview__additional-info">
                    <h3>Additional information</h3>
                    <p>{profile.additionalInfo}</p>
                </div>
            )}
        </section>
    )
}
