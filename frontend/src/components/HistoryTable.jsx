const STATUS = {
  ENROLLED: ["Registered", "ok"],
  PENDING: ["Waiting for approval", "warn"],
  REJECTED: ["Declined", "bad"],
  DROPPED: ["Dropped", ""],
};

export default function HistoryTable({ rows }) {
  if (!rows || rows.length === 0) return <p className="muted">No course registrations yet.</p>;
  return (
    <table>
      <thead><tr><th>Term</th><th>Course</th><th>Status</th><th>Date</th></tr></thead>
      <tbody>
        {rows.map((h, i) => {
          const [text, cls] = STATUS[h.status] || ["", ""];
          return (
            <tr key={i}>
              <td>{h.term}</td>
              <td><strong>{h.courseCode}</strong> {h.courseName}</td>
              <td><span className={`pill ${cls}`}>{text}</span></td>
              <td>{new Date(h.enrolledAt).toLocaleDateString()}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
