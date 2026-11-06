import { z } from "zod";

const policyNumberRegex = /^POL-MT-\d{6}$/;
const e164Regex = /^\+[1-9]\d{6,14}$/;

export const createClaimSchema = z.object({
  policyNumber: z
    .string()
    .min(1, "Policy number is required")
    .regex(policyNumberRegex, "Must match POL-MT-###### (e.g. POL-MT-100001)"),
  claimantName: z.string().min(1, "Claimant name is required").max(150, "Max 150 characters"),
  claimantPhone: z
    .string()
    .min(1, "Phone is required")
    .regex(e164Regex, "Use E.164 format (e.g. +35679123456)"),
  claimantEmail: z.string().min(1, "Email is required").email("Enter a valid email address"),
  vehicleRegistration: z.string().min(1, "Vehicle registration is required").max(15, "Max 15 characters"),
  incidentDate: z
    .string()
    .min(1, "Incident date is required")
    .refine((val) => {
      if (!val) return false;
      return new Date(val).getTime() <= Date.now();
    }, "Incident date cannot be in the future"),
  incidentTime: z.string().optional(),
  incidentLocation: z.string().min(1, "Incident location is required").max(300, "Max 300 characters"),
  incidentDescription: z
    .string()
    .min(20, "Description must be at least 20 characters")
    .max(5000, "Description must be at most 5000 characters"),
  thirdPartyInvolved: z.boolean(),
  policeReportNumber: z.string().max(40, "Max 40 characters").optional().or(z.literal("")),
  injuriesReported: z.boolean(),
  estimatedAmount: z
    .union([z.number(), z.string(), z.nan(), z.undefined()])
    .transform((v) => {
      if (v === undefined || v === "" || (typeof v === "number" && Number.isNaN(v))) return undefined;
      return typeof v === "number" ? v : Number(v);
    })
    .refine((v) => v === undefined || v >= 0, "Must be zero or greater")
    .refine((v) => v === undefined || /^\d+(\.\d{1,2})?$/.test(String(v)), "Max two decimal places")
    .optional(),
});

export type CreateClaimFormValues = z.infer<typeof createClaimSchema>;
export type CreateClaimFormInput = z.input<typeof createClaimSchema>;
export { policyNumberRegex, e164Regex };
