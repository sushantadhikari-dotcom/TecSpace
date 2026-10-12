import { QRCodeSVG } from "qrcode.react";

export default function VirtualLibraryCard({ card, student }) {
  return (
    <div className="libcard">
      <div className="libcard-top">
        <span className="libcard-brand">TecSpace Library</span>
        <span className={`pill ${card.status === "ACTIVE" ? "ok" : "bad"}`}>{card.status}</span>
      </div>

      <div className="libcard-mid">
        <div>
          <h2>{student.firstName} {student.lastName}</h2>
          <p>{student.faculty}</p>
          <p className="mono">{card.cardNumber}</p>
        </div>
        <div className="libcard-qr">
          <QRCodeSVG value={card.qrCodePayload || card.cardNumber} size={76} />
        </div>
      </div>

      <div className="libcard-bottom">
        <span>Issued {card.issueDate}</span>
        <span>Expires {card.expiryDate}</span>
      </div>
    </div>
  );
}
