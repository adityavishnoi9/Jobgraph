import React from 'react'
import './Hero.css'

export default function Hero() {
    return (
        <section className="hero">
            <span className="hero__eyebrow">job application automation</span>
            <h1 className="hero__title">
                Job<span>Graph</span>
            </h1>
            <p className="hero__subtitle">
                Connect your inbox, upload your resume, and let the pipeline find,
                filter, and queue the roles worth applying to.
            </p>
        </section>
    )
}
