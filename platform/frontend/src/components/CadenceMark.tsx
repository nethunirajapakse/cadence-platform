// The one deliberate visual moment on the auth screens: a small tick-mark rule
// under the wordmark, evoking a metronome/ruler beat - a nod to "cadence" as a
// weekly rhythm, rather than a generic hero graphic.
export function CadenceMark() {
  return (
    <div className="mb-10">
      <h1 className="font-display text-2xl font-semibold tracking-tight text-ink">
        Cadence
      </h1>
      <svg width="120" height="8" viewBox="0 0 120 8" className="mt-2" aria-hidden="true">
        {Array.from({ length: 15 }).map((_, i) => (
          <rect
            key={i}
            x={i * 8}
            y={i % 4 === 0 ? 0 : 3}
            width="2"
            height={i % 4 === 0 ? 8 : 5}
            fill={i % 4 === 0 ? "#2F6F63" : "#D8DCD6"}
          />
        ))}
      </svg>
    </div>
  );
}
