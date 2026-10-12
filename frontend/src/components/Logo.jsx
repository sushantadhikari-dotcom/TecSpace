export default function Logo({ size = 28 }) {
  return (
    <span className="logo" style={{ fontSize: size }}>
      <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
        <rect x="2" y="2" width="28" height="28" rx="7" fill="#0E6B6B" />
        <path d="M9 11h14M16 11v13" stroke="#F2A93B" strokeWidth="3.2" strokeLinecap="round" />
      </svg>
      <span>TecSpace</span>
    </span>
  );
}
