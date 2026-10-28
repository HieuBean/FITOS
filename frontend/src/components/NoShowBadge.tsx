'use client';

interface NoShowBadgeProps {
  probability?: number | null; // 0.0 to 1.0
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
  showPercentage?: boolean;
}

export default function NoShowBadge({
  probability,
  riskLevel,
  showPercentage = true,
}: NoShowBadgeProps) {
  if (probability == null && !riskLevel) {
    return null;
  }

  let level: 'LOW' | 'MEDIUM' | 'HIGH' = riskLevel || 'LOW';
  let percentage = 0;

  if (probability != null) {
    percentage = Math.round(probability * 100);
    if (probability >= 0.6) {
      level = 'HIGH';
    } else if (probability >= 0.3) {
      level = 'MEDIUM';
    } else {
      level = 'LOW';
    }
  }

  const styles = {
    LOW: {
      bg: 'bg-emerald-50 border-emerald-200 text-emerald-700',
      dot: 'bg-emerald-500',
      label: 'Low No-show Risk',
    },
    MEDIUM: {
      bg: 'bg-amber-50 border-amber-200 text-amber-700',
      dot: 'bg-amber-500',
      label: 'Medium Risk',
    },
    HIGH: {
      bg: 'bg-rose-50 border-rose-200 text-rose-700',
      dot: 'bg-rose-500 animate-pulse',
      label: 'High No-show Risk',
    },
  }[level];

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${styles.bg}`}
      title={`AI No-show prediction: ${percentage}% likelihood of cancellation or no-show`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${styles.dot}`} />
      <span>{styles.label}</span>
      {showPercentage && probability != null && (
        <span className="opacity-75 font-normal">({percentage}%)</span>
      )}
    </span>
  );
}
