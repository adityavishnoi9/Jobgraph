import './Hero.css'

export default function Hero() {
  return (
    <section className="hero" aria-labelledby="hero-title">
      <div className="hero__content">
        <span className="eyebrow hero__eyebrow"><span className="live-dot" /> A MORE HUMAN WAY TO MOVE FORWARD</span>
        <h1 id="hero-title">Your career has a <em>story.</em><br />Let’s connect the dots.</h1>
        <p className="hero__subtitle">Your experience is more than a document. Turn it into a clear profile, find the signal in job alerts, and move toward work that fits.</p>
        <div className="hero__actions">
          {/* Signup is the entry point before resume upload. */}
          <a className="button button--dark" href="/signup">Build my profile <span aria-hidden="true">↗</span></a>
          <a className="text-link" href="#how-it-works">See how it works <span aria-hidden="true">↘</span></a>
        </div>
        <p className="hero__note"><span aria-hidden="true">✳</span> Start with a resume. See the bigger picture.</p>
      </div>
      <div className="hero-visual" aria-label="Illustration of a resume connected to skills, experience, and opportunities" role="img">
        <div className="hero-visual__top"><span>THE BIG PICTURE</span><span>JG / 001</span></div>
        <div className="orbit orbit--outer" /><div className="orbit orbit--inner" />
        <svg className="hero-visual__lines" viewBox="0 0 520 460" aria-hidden="true">
          <path d="M260 225 L105 100 M260 225 L423 112 M260 225 L115 350 M260 225 L421 350" />
          <circle cx="260" cy="225" r="4" />
        </svg>
        <div className="visual-node visual-node--top-left"><span className="visual-node__icon">✳</span><span>SKILLS</span></div>
        <div className="visual-node visual-node--top-right"><span className="visual-node__icon">↗</span><span>GROWTH</span></div>
        <div className="visual-node visual-node--bottom-left"><span className="visual-node__icon">◈</span><span>PROJECTS</span></div>
        <div className="visual-node visual-node--bottom-right"><span className="visual-node__icon">▤</span><span>EXPERIENCE</span></div>
        <div className="visual-center"><span className="visual-center__spark">✦</span><strong>YOU</strong><small>at the center</small></div>
        <div className="hero-visual__bottom"><span>CONNECT WHAT MATTERS</span><span>● ● ●</span></div>
      </div>
    </section>
  )
}
