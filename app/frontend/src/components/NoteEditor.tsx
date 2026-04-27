interface Props {
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
}

export default function NoteEditor({ value, onChange, disabled }: Props) {
  return (
    <div>
      <label htmlFor="notes">Noter til eksaminator</label>
      <textarea
        id="notes"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        placeholder="Fx: Rapporten indeholder 4 bilag. Virksomhedsnavnet er anonymiseret."
      />
    </div>
  );
}
