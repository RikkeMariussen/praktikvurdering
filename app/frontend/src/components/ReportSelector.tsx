interface Props {
  reports: string[];
  selected: string;
  onChange: (value: string) => void;
  disabled: boolean;
}

export default function ReportSelector({ reports, selected, onChange, disabled }: Props) {
  return (
    <div>
      <label htmlFor="report-select">Rapport</label>
      <select
        id="report-select"
        value={selected}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      >
        <option value="">— vælg rapport —</option>
        {reports.map((r) => (
          <option key={r} value={r}>
            {r}
          </option>
        ))}
      </select>
    </div>
  );
}
