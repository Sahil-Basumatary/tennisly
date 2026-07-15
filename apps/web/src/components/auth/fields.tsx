import { cn } from "@/lib/utils";

type FieldProps = {
  id: string;
  label: string;
  required?: boolean;
  error?: string;
  className?: string;
  children: React.ReactNode;
};

export function AuthField({
  id,
  label,
  required,
  error,
  className,
  children,
}: FieldProps) {
  return (
    <div className={cn("w-full", className)}>
      <label
        htmlFor={id}
        className="mb-1.5 block font-sans text-[14px] font-medium text-[#121619]"
      >
        {label}
        {required ? <span className="text-[#da1e28]"> *</span> : null}
      </label>
      {children}
      {error ? (
        <p className="mt-1 font-sans text-[12px] text-[#da1e28]" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}

const controlClass =
  "h-11 w-full rounded-lg border bg-white px-3 font-sans text-[14px] text-[#121619] outline-none transition-colors placeholder:text-[#697077] focus:border-[#006633] focus:ring-1 focus:ring-[#006633]";

export function authControlClass(hasError?: boolean) {
  return cn(
    controlClass,
    hasError
      ? "border-[#da1e28] focus:border-[#da1e28] focus:ring-[#da1e28]"
      : "border-[#b6bdc5]",
  );
}

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  hasError?: boolean;
};

export function AuthInput({ hasError, className, ...props }: InputProps) {
  return (
    <input className={cn(authControlClass(hasError), className)} {...props} />
  );
}

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement> & {
  hasError?: boolean;
};

export function AuthSelect({ hasError, className, children, ...props }: SelectProps) {
  return (
    <select className={cn(authControlClass(hasError), className)} {...props}>
      {children}
    </select>
  );
}

export function AuthAlert({ children }: { children: React.ReactNode }) {
  if (!children) return null;
  return (
    <p className="font-sans text-[13px] text-[#da1e28]" role="alert">
      {children}
    </p>
  );
}

export function TermsBox({
  id,
  checked,
  onChange,
  error,
}: {
  id: string;
  checked: boolean;
  onChange: (next: boolean) => void;
  error?: string;
}) {
  return (
    <div
      className={cn(
        "rounded-lg border px-3 py-3",
        error ? "border-[#da1e28]" : "border-[#dde1e6]",
      )}
    >
      <label
        htmlFor={id}
        className="flex cursor-pointer items-start gap-3 font-sans text-[13px] text-[#121619]"
      >
        <input
          id={id}
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          className="mt-0.5 h-4 w-4 accent-[#006633]"
        />
        <span>
          <span className="text-[#da1e28]">*</span> I have read, understand and
          agree to the{" "}
          <a
            href="/terms"
            className="font-medium text-[#006633] underline underline-offset-2 hover:text-[#004225]"
          >
            Terms of Use
          </a>
        </span>
      </label>
      {error ? (
        <p className="mt-2 font-sans text-[12px] text-[#da1e28]" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}

export function AuthPrimaryButton({
  children,
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      type="submit"
      className={cn(
        "h-11 w-full rounded-lg bg-[#006633] font-sans text-[12px] font-bold uppercase tracking-wide text-white transition-colors hover:bg-[#004225] disabled:cursor-not-allowed disabled:opacity-60",
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
